package sensor.mocks;

import java.rmi.RemoteException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sensor.base.FutureResult;
import sensor.base.FutureResultImpl;
import sensor.base.SensorParameter;
import sensor.base.SensorServer;
import sensor.base.SensorState;
import sensor.interfaces.TempSensor;

public class Temp2000 extends SensorServer implements TempSensor {
	private static final long serialVersionUID = -9066863232278842877L;
	private Random r;
	private ExecutorService executor;

	@SensorParameter(userDescription = "Unit of measure", propertyName = "unit")
	public String sensorUnit;

	public Temp2000() throws RemoteException {
		super();
	}

	private Double measure(Unit unit) {
		switch (state) {
		case FAULT:
			throw new IllegalStateException("Sensor fault");
		case SHUTDOWN:
			throw new IllegalStateException("Sensor shutdown");
		default:
			break;
		}

		try {
			Thread.sleep(r.nextInt(5000) + 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return Unit.convert(r.nextDouble() * r.nextInt(500), Unit.valueOf(sensorUnit), unit);
	}

	@Override
	public synchronized Double readTemperature(Unit unit) throws RemoteException {
		return readTemperatureAsync(unit).get();
	}

	@Override
	public synchronized FutureResult<Double> readTemperatureAsync(Unit unit) throws RemoteException {
		FutureResultImpl<Double> result = new FutureResultImpl<>();
		CompletableFuture.supplyAsync(() -> measure(unit), executor).thenAccept((value) -> result.set(value));
		return result;
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		sensorUnit = "KELVIN";
		r = new Random();
		executor = Executors.newFixedThreadPool(1);
		state = SensorState.RUNNING;
	}

	@Override
	public void tearDown() {
		super.tearDown();
		executor.shutdown();
	}

}
