package sensor.implementations;

import java.rmi.RemoteException;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sensor.FutureResult;
import sensor.FutureResultImpl;
import sensor.TempSensor;
import sensor.TempSensor.Unit;

public class Temp2000 implements TempSensor {

	private transient Random r;
	private transient ExecutorService executor;
	private transient Unit sensorUnit;

	public Temp2000() throws RemoteException {
		r = new Random();
		executor = Executors.newFixedThreadPool(1);
		sensorUnit = Unit.KELVIN;
	}

	private Double measure(Unit unit) {
		try {
			Thread.sleep(r.nextInt(5000) + 1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Unit.convert(r.nextDouble() * r.nextInt(500), sensorUnit, unit);
	}

	@Override
	public synchronized Double readTemperature(Unit unit) throws RemoteException {
		return readTemperatureAsync(unit).get();
	}

	@Override
	public synchronized FutureResult<Double> readTemperatureAsync(Unit unit) throws RemoteException {
		FutureResultImpl<Double> result = new FutureResultImpl<>();
		(CompletableFuture.supplyAsync(() -> measure(unit), executor)).thenAccept((value) -> result.set(value));
		return result;
	}
}
