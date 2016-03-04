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

public class Temp4000 extends SensorServer implements TempSensor {
	private static final long serialVersionUID = -9066863232278842877L;
	private Random r;
	private ExecutorService executor;
	private FutureResultImpl<Double> result;
	private Runnable measurer = new Runnable() {
		@Override
		public void run() {
			state.setState(State.MEASURING);
			try {
				Thread.sleep(r.nextInt(5000) + 1000);
				result.set(r.nextDouble() * 1000);
				System.out.println("Measure done");
				state.setState(State.RUNNING);
			} catch (InterruptedException e) {
				System.out.println("A measure failed");
				state.setState(State.FAULT);
				state.setComment("A measure failed");
				result.raiseException(e);
			}
		}
	};
	private Runnable invalidator = new Runnable() {
		@Override
		public void run() {
			try {
				Thread.sleep(invalidateResultAfter * 1000);
			} catch (InterruptedException ignore) {
			}
			result = null;
			System.out.println("Measure invalidated");
		}
	};

	@SensorParameter(userDescription = "Amount of time in seconds after which a new measurement is needed", propertyName = "InvalidateResultAfter")
	public Long invalidateResultAfter;

	public Temp4000() throws RemoteException {
		super();
		this.result = null;
	}

	@Override
	public synchronized Double readTemperature(Unit unit) throws RemoteException {
		return readTemperatureAsync(unit).get();
	}

	@Override
	public synchronized FutureResult<Double> readTemperatureAsync(Unit unit) throws RemoteException {
		// if a measure is already running, return the same FutureResult to
		// everyone requesting, it will be updated as soon as the measure ends
		if (result == null) {
			result = new FutureResultImpl<>();
			CompletableFuture.runAsync(measurer, executor).thenRunAsync(invalidator,executor);
		}
		return result;

	}

	@Override
	public void setUp() {
		if (!allParametersFilledUp()) {
			state.setState(State.SETUP);
			state.setComment("Set up");
		} else {
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
		System.out.println("Tem42000 stopped");
	}
}
