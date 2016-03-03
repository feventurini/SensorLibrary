package implementations;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.iot.raspberry.grovepi.devices.GroveTemperatureAndHumiditySensor;
import org.iot.raspberry.grovepi.devices.GroveTemperatureAndHumidityValue;
import org.iot.raspberry.grovepi.pi4j.GrovePi4J;

import sensor.FutureResult;
import sensor.FutureResultImpl;
import sensor.HumiditySensor;
import sensor.SensorParameter;
import sensor.SensorState.State;
import sensor.TempSensor;

public class GrovePiTempAndHumiditySensor extends SensorServer implements TempSensor, HumiditySensor {
	private static final long serialVersionUID = -5353227817012312834L;
	private GroveTemperatureAndHumiditySensor sensor;
	private GroveTemperatureAndHumidityValue lastMeasure;
	private long lastMeasureTime;
	private Measurer measurer;

	private ExecutorService executor;

	@SensorParameter(userDescription = "Amount of time after which a new measurement is needed", propertyName = "InvalidateResultAfter")
	private Long invalidateResultAfter;

	private class Measurer implements Runnable {

		public void run() {
			state.setState(State.MEASURING);
			try {
				lastMeasure = sensor.get();
			} catch (IOException e) {
				state.setState(State.FAULT);
				state.setComment("A measure failed");
				return;
			}
			lastMeasureTime = System.currentTimeMillis();
			state.setState(State.RUNNING);
			synchronized (state) {
				state.notifyAll();
			}
		}

	}

	public GrovePiTempAndHumiditySensor() throws RemoteException {
		super();
	}

	private void measure() {
		synchronized (state) {
			switch (state.getState()) {
			case SETUP:
				throw new IllegalStateException("Sensor setup incomplete");
			case FAULT:
				throw new IllegalStateException("Sensor fault");
			case SHUTDOWN:
				throw new IllegalStateException("Sensor shutdown");
			case MEASURING:
				try {
					state.wait();
				} catch (InterruptedException ignore) {
					ignore.printStackTrace();
				}
			case RUNNING:
				if (System.currentTimeMillis() - lastMeasureTime < invalidateResultAfter)
					return;
				else
					executor.submit(measurer);
				break;
			}
		}
	}

	@Override
	public synchronized Double readTemperature(Unit unit) throws RemoteException {
		return readTemperatureAsync(unit).get();
	}

	@Override
	public synchronized FutureResult<Double> readTemperatureAsync(Unit unit) throws RemoteException {
		FutureResultImpl<Double> result = new FutureResultImpl<>();
		CompletableFuture.supplyAsync(() -> {
			measure();
			if (state.getState() == State.FAULT) {
				result.raiseException(new IOException("Measure Failed"));
				return 0.0;
			}
			return Unit.convert(lastMeasure.getTemperature(), Unit.CELSIUS, unit);
		} , executor).thenAccept((value) -> result.set(value));
		return result;
	}

	@Override
	public Double readHumidity() throws RemoteException {
		return readHumidityAsync().get();
	}

	@Override
	public FutureResult<Double> readHumidityAsync() throws RemoteException {
		FutureResultImpl<Double> result = new FutureResultImpl<>();
		(CompletableFuture.supplyAsync(() -> {
			measure();
			if (state.getState() == State.FAULT) {
				result.raiseException(new IOException("Measure Failed"));
				return 0.0;
			}
			return lastMeasure.getHumidity();
		} , executor)).thenAccept((value) -> result.set(value));
		return result;

	}

	@Override
	public void setUp() throws Exception {
		if (!allParametersFilledUp()) {
			state.setState(State.SETUP);
		} else {
			try {
				this.sensor = new GroveTemperatureAndHumiditySensor(new GrovePi4J(), 2,
						GroveTemperatureAndHumiditySensor.Type.DHT11);
			} catch (IOException e) {
				e.printStackTrace();
			}
			executor = Executors.newFixedThreadPool(1);
			state.setState(State.RUNNING);
			lastMeasureTime = -1;
			measurer = new Measurer();
		}
	}

	@Override
	public void tearDown() {
		state.setState(State.SHUTDOWN);
	}

}