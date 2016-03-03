package implementations;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.iot.raspberry.grovepi.GrovePi;
import org.iot.raspberry.grovepi.devices.GroveTemperatureAndHumiditySensor;
import org.iot.raspberry.grovepi.pi4j.GrovePi4J;

import sensor.FutureResult;
import sensor.FutureResultImpl;
import sensor.TempSensor;
import sensor.TempSensor.Unit;

public class GrovePiTempSensor extends SensorServer implements TempSensor {

	private GroveTemperatureAndHumiditySensor sensor;
	private ExecutorService executor;

	public GrovePiTempSensor() throws RemoteException {
		super();
	}

	private double measure() throws IOException {
		sensor.get().getTemperature();
	}

	@Override
	public synchronized Double readTemperature(Unit unit) throws RemoteException {
		return readTemperatureAsync(unit).get();
	}

	@Override
	public synchronized FutureResult<Double> readTemperatureAsync(Unit unit) throws RemoteException {
		FutureResultImpl<Double> result = new FutureResultImpl<>();
		(CompletableFuture.supplyAsync(() -> measure(), executor)).exceptionally()
				.thenAccept((value) -> result.set(value));
		return result;
	}

	@Override
	public void setUp() throws Exception {
		if (!allParametersFilledUp()) {
			isSetUp = false;
		} else {
			try {
				this.sensor = new GroveTemperatureAndHumiditySensor(new GrovePi4J(), 2,
						GroveTemperatureAndHumiditySensor.Type.DHT11);
			} catch (IOException e) {
				e.printStackTrace();
			}
			executor = Executors.newFixedThreadPool(1);
		}
	}

	@Override
	public void tearDown() {
		// TODO Auto-generated method stub

	}

}
