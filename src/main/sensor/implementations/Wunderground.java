package sensor.implementations;

import java.io.IOException;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
import sensor.interfaces.AstronomySensor;
import sensor.interfaces.WeatherSensor;

public class Wunderground extends SensorServer implements WeatherSensor, AstronomySensor {
	public class AstronomyHandler extends DefaultHandler {
		private Astronomy observation;
		private boolean in_moonphase;
		private boolean in_sunphase;
		private boolean in_sunset;
		private boolean in_sunrise;
		private boolean in_moonset;
		private boolean in_moonrise;
		private boolean in_hour;
		private boolean in_minute;

		public AstronomyHandler(Astronomy observation) {
			this.observation = observation;
			Calendar now = new GregorianCalendar();
			this.observation.moonrise = now;
			this.observation.moonset = now;
			this.observation.sunrise = now;
			this.observation.sunset = now;
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			// se fallisce o viene saltata la lettura di uno o più campi si va
			// avanti
			try {
				if (in_moonphase && in_moonrise && in_hour) {
					observation.moonrise.set(Calendar.HOUR_OF_DAY, Integer.parseInt(new String(ch, start, length)));
				} else if (in_moonphase && in_moonrise && in_minute) {
					observation.moonrise.set(Calendar.MINUTE, Integer.parseInt(new String(ch, start, length)));
				} else if (in_moonphase && in_moonset && in_hour) {
					observation.moonset.set(Calendar.HOUR_OF_DAY, Integer.parseInt(new String(ch, start, length)));
				} else if (in_moonphase && in_moonset && in_minute) {
					observation.moonset.set(Calendar.MINUTE, Integer.parseInt(new String(ch, start, length)));
				} else if (in_sunphase && in_sunrise && in_hour) {
					observation.sunrise.set(Calendar.HOUR_OF_DAY, Integer.parseInt(new String(ch, start, length)));
				} else if (in_sunphase && in_sunrise && in_minute) {
					observation.sunrise.set(Calendar.MINUTE, Integer.parseInt(new String(ch, start, length)));
				} else if (in_sunphase && in_sunset && in_hour) {
					observation.sunset.set(Calendar.HOUR_OF_DAY, Integer.parseInt(new String(ch, start, length)));
				} else if (in_sunphase && in_sunset && in_minute) {
					observation.sunset.set(Calendar.MINUTE, Integer.parseInt(new String(ch, start, length)));
				}
			} catch (NumberFormatException e) {
				log.log(Level.WARNING, "Failed to parse a value from xml", e);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (qName.equals("moonrise")) {
				in_moonrise = false;
			} else if (qName.equals("moonset")) {
				in_moonset = false;
			} else if (qName.equals("sunrise")) {
				in_sunrise = false;
			} else if (qName.equals("sunset")) {
				in_sunset = false;
			} else if (qName.equals("sunphase")) {
				in_sunphase = false;
			} else if (qName.equals("moonphase")) {
				in_moonphase = false;
			} else if (qName.equals("hour")) {
				in_hour = false;
			} else if (qName.equals("minute")) {
				in_minute = false;
			}
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			if (qName.equals("moonrise")) {
				in_moonrise = true;
			} else if (qName.equals("moonset")) {
				in_moonset = true;
			} else if (qName.equals("sunrise")) {
				in_sunrise = true;
			} else if (qName.equals("sunset")) {
				in_sunset = true;
			} else if (qName.equals("sunphase")) {
				in_sunphase = true;
			} else if (qName.equals("moonphase")) {
				in_moonphase = true;
			} else if (qName.equals("hour")) {
				in_hour = true;
			} else if (qName.equals("minute")) {
				in_minute = true;
			}
		}
	}

	private class WeatherHandler extends DefaultHandler {

		private Weather observation;
		private boolean in_mmRain;
		private boolean in_pressure;
		private boolean in_windDegrees;
		private boolean in_windSpeed;
		private boolean in_feelsLike;
		private boolean in_temp_c;

		public WeatherHandler(Weather observation) {
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

	private FutureResultImpl<Weather> weather;
	private FutureResultImpl<Astronomy> astronomy;
	private ExecutorService executor;
	private URL weatherUrl;
	private URL astronomyUrl;
	private AtomicInteger errorCounter;
	private final int errorTreshold = 20;

	@SensorParameter(userDescription = "Amount of seconds after which a new weather measurement is needed", propertyName = "InvalidateResultAfter")
	public Long invalidateResultAfter;
	@SensorParameter(userDescription = "City", propertyName = "City")
	public String city;
	@SensorParameter(userDescription = "State", propertyName = "State")
	public String stat;
	@SensorParameter(userDescription = "API key", propertyName = "Key")
	public String key;

	private Supplier<Weather> weatherMeasurer = () -> {
		try {
			Weather observation = new Weather();

			HttpURLConnection conn = (HttpURLConnection) weatherUrl.openConnection();
			int response = conn.getResponseCode();
			if (response != HttpURLConnection.HTTP_OK)
				throw new CompletionException(new HttpRetryException("HTTP response code " + response, response));

			SAXParser sax = SAXParserFactory.newInstance().newSAXParser();
			sax.parse(conn.getInputStream(), new WeatherHandler(observation));

			log.info("Weather response: " + observation.toString());
			errorCounter.set(0); // reset error counter
			return observation;
		} catch (ParserConfigurationException | SAXException e1) {
			log.log(Level.SEVERE, "Parser error", e1);
			throw new CompletionException(e1);
		} catch (IOException e2) {
			log.log(Level.WARNING, "A weather request failed", e2);
			if (errorCounter.incrementAndGet() >= errorTreshold) {
				log.severe("Wunderground state set to FAULT because of repeated failures");
				fail();
			}
			throw new CompletionException(e2);
		}
	};

	private Runnable weatherInvalidator = () -> {
		try {
			Thread.sleep(invalidateResultAfter * 1000);
		} catch (InterruptedException ignore) {
		}
		weather = null;
		log.info("Measure invalidated");
	};

	private Supplier<Astronomy> astronomyMeasurer = () -> {
		try {
			Astronomy observation = new Astronomy();

			HttpURLConnection conn = (HttpURLConnection) astronomyUrl.openConnection();
			int response = conn.getResponseCode();
			if (response != HttpURLConnection.HTTP_OK)
				throw new CompletionException(new HttpRetryException("HTTP response code " + response, response));

			SAXParser sax = SAXParserFactory.newInstance().newSAXParser();
			sax.parse(conn.getInputStream(), new AstronomyHandler(observation));

			log.info("Weather response: " + observation.toString());
			errorCounter.set(0); // reset error counter
			return observation;
		} catch (ParserConfigurationException | SAXException e1) {
			log.log(Level.SEVERE, "Parser error", e1);
			throw new CompletionException(e1);
		} catch (IOException e2) {
			log.log(Level.WARNING, "An astronomy request failed", e2);
			if (errorCounter.incrementAndGet() >= errorTreshold) {
				log.severe("Wunderground state set to FAULT because of repeated failures");
				fail();
			}
			throw new CompletionException(e2);
		}
	};

	private Runnable astronomyInvalidator = () -> {
		weather = null;
		log.info("Measure invalidated");
	};

	public Wunderground() throws RemoteException {
		super();
	}

	@Override
	public synchronized Astronomy getAstronomy() throws RemoteException {
		return getAstronomyAsync().get();
	}

	@Override
	public synchronized FutureResult<Astronomy> getAstronomyAsync() throws RemoteException {
		// if a measure is already running, return the same FutureResult to
		// everyone requesting, it will be updated as soon as the measure ends
		switch (getState()) {
		case FAULT:
			throw new IllegalStateException("Sensor fault");
		case SHUTDOWN:
			throw new IllegalStateException("Sensor shutdown");
		default:
			if (astronomy == null) {
				astronomy = new FutureResultImpl<>();
				CompletableFuture.supplyAsync(astronomyMeasurer, executor).exceptionally((ex) -> {
					astronomy.raiseException((Exception) ex.getCause());
					return null; // questo valore verrà ignorato
				}).thenAccept(astronomy::set);
			}
			return astronomy;
		}
	}

	@Override
	public synchronized Weather getWeather() throws RemoteException {
		return getWeatherAsync().get();
	}

	@Override
	public synchronized FutureResult<Weather> getWeatherAsync() throws RemoteException {
		// if a measure is already running, return the same FutureResult to
		// everyone requesting, it will be updated as soon as the measure ends
		switch (getState()) {
		case FAULT:
			throw new IllegalStateException("Sensor fault");
		case SHUTDOWN:
			throw new IllegalStateException("Sensor shutdown");
		default:
			if (weather == null) {
				weather = new FutureResultImpl<>();
				CompletableFuture.supplyAsync(weatherMeasurer, executor).exceptionally((ex) -> {
					weather.raiseException((Exception) ex.getCause());
					return null; // questo valore verrà ignorato
				}).thenAccept(weather::set).thenRunAsync(weatherInvalidator, executor);
			}
			return weather;
		}
	}

	@Override
	public void customSetUp() throws Exception {
		try {
			weatherUrl = new URL(
					"http://api.wunderground.com/api/" + key + "/conditions/q/" + stat + "/" + city + ".xml");
			astronomyUrl = new URL(
					"http://api.wunderground.com/api/" + key + "/astronomy/q/" + stat + "/" + city + ".xml");
		} catch (MalformedURLException e) {
			log.log(Level.SEVERE, "Error creating wunderground url", e);
			throw e;
		}
		errorCounter = new AtomicInteger();
		weather = null;
		executor = Executors.newFixedThreadPool(2);

		LocalTime midnight = LocalTime.MIDNIGHT;
		LocalDate today = LocalDate.now();
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime tomorrow_00_00 = LocalDateTime.of(today, midnight).plusDays(1);

		int oneDay = 24 * 60 * 60;
		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(astronomyInvalidator,
				now.until(tomorrow_00_00, ChronoUnit.SECONDS), oneDay, TimeUnit.SECONDS);
	}

	@Override
	public void customTearDown() {
		if (executor != null) {
			executor.shutdown();
		}
	}

	@Override
	protected void customFail() {
		// TODO Auto-generated method stub
		
	}
}
