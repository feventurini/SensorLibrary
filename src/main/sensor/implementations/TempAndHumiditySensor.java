package sensor.implementations;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.iot.raspberry.grovepi.devices.GroveTemperatureAndHumiditySensor;
import org.iot.raspberry.grovepi.devices.GroveTemperatureAndHumidityValue;
import org.iot.raspberry.grovepi.pi4j.GrovePi4J;

import sensor.base.FutureResult;
import sensor.base.FutureResultImpl;
import sensor.base.SensorParameter;
import sensor.base.SensorServer;
import sensor.base.SensorState;
import sensor.interfaces.HumiditySensor;
import sensor.interfaces.TempSensor;

public class TempAndHumiditySensor extends SensorServer implements TempSensor, HumiditySensor {
	private static final long serialVersionUID = -5353227817012312834L;

	private final static Logger log = Logger.getLogger(TempAndHumiditySensor.class.getName());

	private GroveTemperatureAndHumiditySensor sensor;
	private FutureResultImpl<Double> resultTemp;
	private FutureResultImpl<Double> resultHumid;

	private ExecutorService executor;

	@SensorParameter(userDescription = "Amount of seconds after which a new measurement is needed", propertyName = "InvalidateResultAfter")
	public Long invalidateResultAfter;

	private Supplier<GroveTemperatureAndHumidityValue> measurer = () -> {
		try {
			GroveTemperatureAndHumidityValue value = sensor.get();
			log.info("Measure done: " + value);
			return value;
		} catch (IOException e) {
			log.log(Level.SEVERE, "A measure failed", e);
			setState(SensorState.FAULT);
			throw new CompletionException(e);
		}
	};

	private Runnable invalidator = () -> {
		try {
			Thread.sleep(invalidateResultAfter * 1000);
		} catch (InterruptedException ignore) {
		}
		resultTemp = null;
		resultHumid = null;
		log.info("Measure invalidated");
	};

	public TempAndHumiditySensor() throws RemoteException {
		super();
		resultHumid = null;
		resultTemp = null;
	}

	@Override
	public Double readHumidity() throws RemoteException {
		return readHumidityAsync().get();
	}

	@Override
	public FutureResult<Double> readHumidityAsync() throws RemoteException {
		// if a measure is already running, return the same FutureResult to
		// everyone requesting, it will be updated as soon as the measure ends
		switch (getState()) {
		case FAULT:
			throw new IllegalStateException("Sensor fault");
		case SHUTDOWN:
			throw new IllegalStateException("Sensor shutdown");
		default:
			if (resultHumid == null) {
				resultHumid = new FutureResultImpl<>();
				CompletableFuture.supplyAsync(measurer, executor).exceptionally((ex) -> {
					resultHumid.raiseException((Exception) ex.getCause());
					return null; // questo valore verrà ignorato
				}).thenAccept((reading) -> resultTemp.set(reading.getHumidity())).thenRunAsync(invalidator, executor);
			}
			return resultHumid;
		}
	}

	@Override
	public synchronized Double readTemperature(Unit unit) throws RemoteException {
		return readTemperatureAsync(unit).get();
	}

	@Override
	public synchronized FutureResult<Double> readTemperatureAsync(Unit unit) throws RemoteException {
		// if a measure is already running, return the same FutureResult to
		// everyone requesting, it will be updated as soon as the measure ends
		switch (getState()) {
		case FAULT:
			throw new IllegalStateException("Sensor fault");
		case SHUTDOWN:
			throw new IllegalStateException("Sensor shutdown");
		default:
			if (resultTemp == null) {
				resultTemp = new FutureResultImpl<>();
				CompletableFuture.supplyAsync(measurer, executor).exceptionally((ex) -> {
					resultTemp.raiseException((Exception) ex.getCause());
					return null; // questo valore verrà ignorato
				}).thenAccept((reading) -> resultTemp.set(reading.getTemperature())).thenRunAsync(invalidator,
						executor);
			}
			return resultTemp;
		}
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		try {
			sensor = new GroveTemperatureAndHumiditySensor(new GrovePi4J(), 2,
					GroveTemperatureAndHumiditySensor.Type.DHT11);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error connecting to the sensor", e);
		}
		executor = Executors.newFixedThreadPool(1);
	}

	@Override
	public void tearDown() {
		super.tearDown();
		executor.shutdown();
	}

}
