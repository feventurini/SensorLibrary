package sensor.mocks;

import java.rmi.RemoteException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import sensor.base.FutureResult;
import sensor.base.FutureResultImpl;
import sensor.base.SensorParameter;
import sensor.base.SensorServer;
import sensor.interfaces.TempSensor;

public class Temp4000 extends SensorServer implements TempSensor {
	private static final Logger log = Logger.getLogger(Temp4000.class.getName());
	private static final long serialVersionUID = -9066863232278842877L;

	public static void main(String[] args) throws RemoteException {
		new Temp4000();
	}

	@SensorParameter(userDescription = "Amount of time in seconds after which a new measurement is needed", propertyName = "InvalidateResultAfter")
	public Long invalidateResultAfter;
	private Random r;
	private ExecutorService executor;
	private FutureResultImpl<Double> result;

	private Supplier<Double> measurer = () -> {
		try {
			Thread.sleep(r.nextInt(5000) + 1000);
			log.info("Measure done");
			return r.nextDouble() * 1000;
		} catch (InterruptedException e) {
			log.log(Level.WARNING, "Sensor interrupted while measuring", e);
			fail();
			throw new CompletionException(e);
		}
	};

	private Runnable invalidator = () -> {
		try {
			Thread.sleep(invalidateResultAfter * 1000);
		} catch (InterruptedException e) {
			log.log(Level.WARNING, "Sensor interrupted while measuring", e);
		}
		result = null;
		log.info("Measure invalidated");

	};

	public Temp4000() throws RemoteException {
		super();
		result = null;
	}

	@Override
	public synchronized Double readTemperature() throws RemoteException {
		return readTemperatureAsync().get();
	}

	@Override
	public synchronized FutureResult<Double> readTemperatureAsync() throws RemoteException {
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
	public void customSetUp() throws Exception {
		r = new Random();
		executor = Executors.newFixedThreadPool(1);
	}

	@Override
	public void customTearDown() {
		executor.shutdown();
	}

	@Override
	protected void customFail() {
	}
}
