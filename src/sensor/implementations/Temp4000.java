package sensor.implementations;

import java.rmi.RemoteException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import sensor.FutureResult;
import sensor.FutureResultImpl;
import sensor.SensorParameter;
import sensor.SensorServer;
import sensor.SensorState;
import sensor.interfaces.TempSensor;

public class Temp4000 extends SensorServer implements TempSensor {
	private static final long serialVersionUID = -9066863232278842877L;
	@SensorParameter(userDescription = "Amount of time in seconds after which a new measurement is needed", propertyName = "InvalidateResultAfter")
	public Long invalidateResultAfter;
	private Random r;
	private ExecutorService executor;
	private FutureResultImpl<Double> result;
	private Supplier<Double> measurer = () -> {
		try {
			Thread.sleep(r.nextInt(5000) + 1000);
			System.out.println("Measure done");
			return r.nextDouble() * 1000;
		} catch (InterruptedException e)

		{
			System.out.println("A measure failed");
			state = SensorState.FAULT;
			
			throw new CompletionException(e);
		}
	};

	private Runnable invalidator = () -> {
		try {
			Thread.sleep(invalidateResultAfter * 1000);
		} catch (InterruptedException ignore)

		{
		}
		result = null;
		System.out.println("Measure invalidated");

	};

	public Temp4000() throws RemoteException {
		super();
		result = null;
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

			// CON UN SUPPLIER CHE LANCIA ECCEZIONI
			CompletableFuture.supplyAsync(measurer, executor).exceptionally((ex) -> {
				result.raiseException((Exception) ex.getCause());
				return -1.0; // questo valore verrà ignorato
			}).thenAccept((temp) -> result.set(temp)).thenRunAsync(invalidator, executor);

			// CON DUE RUNNABLE
			// CompletableFuture.runAsync(measurer,
			// executor).thenRunAsync(invalidator, executor);
		}
		return result;

	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
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
