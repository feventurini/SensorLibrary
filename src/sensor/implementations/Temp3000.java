package sensor.implementations;

import java.rmi.RemoteException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sensor.TempSensor;

public class Temp3000 implements TempSensor {

	private transient Random r;
	private transient ExecutorService executor;
	private transient Unit sensorUnit;

	public Temp3000() {
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
	public Double readTemperature(Unit unit) throws RemoteException {
		try {
			return readTemperatureAsync(unit).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new RemoteException("InterruptedException", e);
		} catch (ExecutionException e) {
			e.printStackTrace();
			throw new RemoteException("ExecutionException", e);
		}
	}

	@Override
	public CompletableFuture<Double> readTemperatureAsync(Unit unit) throws RemoteException {
		return CompletableFuture.supplyAsync(() -> measure(unit), executor);
	}
}
