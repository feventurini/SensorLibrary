package sensor.implementations;

import java.rmi.RemoteException;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import sensor.TempSensor;

public class Temp2000 implements TempSensor {

	private Random r;
	private ExecutorService executor;
	private Unit sensorUnit;

	public Temp2000() {
		r = new Random();
		executor = Executors.newFixedThreadPool(1);
		sensorUnit = Unit.KELVIN;
	}

	private class Measurer implements Callable<Double> {

		private Unit unit;

		public Measurer(Unit unit) {
			this.unit = unit;
		}

		@Override
		public Double call() throws Exception {
			Thread.sleep(r.nextInt(5000) + 1000);
			return Unit.convert(r.nextDouble() * r.nextInt(500), sensorUnit, unit);
		}

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
	public Future<Double> readTemperatureAsync(Unit unit) throws RemoteException {
		return executor.submit(new Measurer(unit));
	}
}
