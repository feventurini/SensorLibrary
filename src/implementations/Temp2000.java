package implementations;

import java.rmi.RemoteException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sensor.FutureResult;
import sensor.FutureResultImpl;
import sensor.SensorParameter;
import sensor.SensorState.State;
import sensor.TempSensor;

public class Temp2000 extends SensorServer implements TempSensor {
	private static final long serialVersionUID = -9066863232278842877L;
	private Random r;
	private ExecutorService executor;

	@SensorParameter(userDescription = "Unit of measure", propertyName = "unit")
	public Unit sensorUnit;

	@SensorParameter(userDescription = "Delay between measures", propertyName = "delay")
	public Integer delay;

	protected Temp2000() throws RemoteException {
		super();
	}

	private Double measure(Unit unit) {
		switch (state.getState()) {
		case SETUP:
			throw new IllegalStateException("Sensor setup incomplete");
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
		return Unit.convert(r.nextDouble() * r.nextInt(500), sensorUnit, unit);
	}

	@Override
	public synchronized Double readTemperature(Unit unit)
			throws RemoteException {
		return readTemperatureAsync(unit).get();
	}

	@Override
	public synchronized FutureResult<Double> readTemperatureAsync(Unit unit)
			throws RemoteException {
		FutureResultImpl<Double> result = new FutureResultImpl<>();
		(CompletableFuture.supplyAsync(() -> measure(unit), executor))
				.thenAccept((value) -> result.set(value));
		return result;
	}

	@Override
	public void setUp() {
		if (!allParametersFilledUp()) {
			state.setState(State.SETUP);
			state.setComment("Set up");

		} else {
			sensorUnit = Unit.KELVIN;
			r = new Random();
			executor = Executors.newFixedThreadPool(1);
			state.setState(State.RUNNING);
			state.setComment("Running");
		}
	}

	@Override
	public void tearDown() {
		executor.shutdown();
		state.setState(State.SHUTDOWN);
		state.setComment("Shutdown");
		System.out.println("Temp2000 stopped");
	}

}
