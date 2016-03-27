package sensor.implementations;

import java.io.IOException;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import sensor.base.FutureResult;
import sensor.base.FutureResultImpl;
import sensor.base.SensorParameter;
import sensor.base.SensorServer;
import sensor.base.SensorState;
import sensor.interfaces.WeatherSensor;

public class Wunderground extends SensorServer implements WeatherSensor {
	private class WundergroundHandler extends DefaultHandler {

		private Observation observation;
		private boolean in_mmRain;
		private boolean in_pressure;
		private boolean in_windDegrees;
		private boolean in_windSpeed;
		private boolean in_feelsLike;
		private boolean in_temp_c;

		public WundergroundHandler(Observation observation) {
			this.observation = observation;
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			// se fallisce o viene saltata la lettura di uno o più campi si va
			// avanti
			try {
				if (in_temp_c) {
					observation.temp = Double.valueOf(new String(ch, start, length));
				} else if (in_feelsLike) {
					observation.feelsLike = Double.valueOf(new String(ch, start, length));
				} else if (in_windSpeed) {
					observation.windSpeed = Double.valueOf(new String(ch, start, length));
				} else if (in_windDegrees) {
					observation.windDegrees = Double.valueOf(new String(ch, start, length));
				} else if (in_pressure) {
					observation.pressure = Double.valueOf(new String(ch, start, length));
				} else if (in_mmRain) {
					observation.mmRain = Double.valueOf(new String(ch, start, length));
				}
			} catch (NumberFormatException e) {
				log.log(Level.WARNING, "Failed to parse a value from xml", e);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (qName.equals("temp_c")) {
				in_temp_c = false;
			} else if (qName.equals("feelslike_c")) {
				in_feelsLike = false;
			} else if (qName.equals("wind_kph")) {
				in_windSpeed = false;
			} else if (qName.equals("wind_degrees")) {
				in_windDegrees = false;
			} else if (qName.equals("pressure_mb")) {
				in_pressure = false;
			} else if (qName.equals("precip_today_metric")) {
				in_mmRain = false;
			}
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			if (qName.equals("temp_c")) {
				in_temp_c = true;
			} else if (qName.equals("feelslike_c")) {
				in_feelsLike = true;
			} else if (qName.equals("wind_kph")) {
				in_windSpeed = true;
			} else if (qName.equals("wind_degrees")) {
				in_windDegrees = true;
			} else if (qName.equals("pressure_mb")) {
				in_pressure = true;
			} else if (qName.equals("precip_today_metric")) {
				in_mmRain = true;
			}
		}
	}

	private static final long serialVersionUID = -7937251314150401320L;

	private final static Logger log = Logger.getLogger(TempAndHumiditySensor.class.getName());

	private FutureResultImpl<Observation> result;
	private ExecutorService executor;
	private URL url;
	private int errorCounter;
	private final int errorTreshold = 20;

	@SensorParameter(userDescription = "Amount of seconds after which a new measurement is needed", propertyName = "InvalidateResultAfter")
	public Long invalidateResultAfter;
	@SensorParameter(userDescription = "City", propertyName = "City")
	public String city;
	@SensorParameter(userDescription = "State", propertyName = "State")
	public String stat;
	@SensorParameter(userDescription = "API key", propertyName = "Key")
	public String key;
	private Supplier<Observation> measurer = () -> {
		try {
			Observation observation = new Observation();

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			int response = conn.getResponseCode();
			if (response != HttpURLConnection.HTTP_OK)
				throw new CompletionException(new HttpRetryException("HTTP response code " + response, response));

			SAXParser sax = SAXParserFactory.newInstance().newSAXParser();
			sax.parse(conn.getInputStream(), new WundergroundHandler(observation));

			log.info("Weather response: " + observation.toString());
			errorCounter = 0; // reset error counter
			return observation;
		} catch (ParserConfigurationException | SAXException e1) {
			log.log(Level.SEVERE, "Parser error", e1);
			throw new CompletionException(e1);
		} catch (IOException e2) {
			log.log(Level.WARNING, "A weather request failed", e2);
			errorCounter++;
			if (errorCounter >= errorTreshold) {
				state = SensorState.FAULT;
				log.severe("Rfid_SL030 state set to FAULT because of repeated failures");
			}
			throw new CompletionException(e2);
		}
	};

	private Runnable invalidator = () -> {
		try {
			Thread.sleep(invalidateResultAfter * 1000);
		} catch (InterruptedException ignore) {
		}
		result = null;
		log.info("Measure invalidated");
	};

	public Wunderground() throws RemoteException {
		super();
	}

	@Override
	public Observation getObservation() throws RemoteException {
		return getObservationAsync().get();
	}

	@Override
	public FutureResult<Observation> getObservationAsync() throws RemoteException {
		// if a measure is already running, return the same FutureResult to
		// everyone requesting, it will be updated as soon as the measure ends
		switch (state) {
		case FAULT:
			throw new IllegalStateException("Sensor fault");
		case SHUTDOWN:
			throw new IllegalStateException("Sensor shutdown");
		default:
			if (result == null) {
				result = new FutureResultImpl<>();
				CompletableFuture.supplyAsync(measurer, executor).exceptionally((ex) -> {
					result.raiseException((Exception) ex.getCause());
					return null; // questo valore verrà ignorato
				}).thenAccept(result::set).thenRunAsync(invalidator, executor);
			}
			return result;
		}
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		try {
			url = new URL("http://api.wunderground.com/api/" + key + "/conditions/q/" + stat + "/" + city + ".xml");
		} catch (MalformedURLException ignore) {
		}
		errorCounter = 0;
		result = null;
		executor = Executors.newFixedThreadPool(1);
		state = SensorState.RUNNING;
	}

	@Override
	public void tearDown() {
		if (executor != null) {
			executor.shutdown();
		}
		state = SensorState.SHUTDOWN;
	}
}
