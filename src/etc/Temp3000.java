package etc;

import java.rmi.RemoteException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Temp3000 {

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
			e.printStackTrace();
		}
		return Unit.convert(r.nextDouble() * r.nextInt(500), sensorUnit, unit);
	}

	public Double readTemperature(Unit celsius) throws RemoteException {
		try {
			return readTemperatureAsync(celsius).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new RemoteException("InterruptedException", e);
		} catch (ExecutionException e) {
			e.printStackTrace();
			throw new RemoteException("ExecutionException", e);
		}
	}

	public CompletableFuture<Double> readTemperatureAsync(Unit unit) throws RemoteException {
		return CompletableFuture.supplyAsync(() -> measure(unit), executor);
	}

	public enum Unit {
		CELSIUS, FAHRENHEIT, KELVIN;
		public static Double convert(Double value, Unit from, Unit to) {
			switch (from) {
			case CELSIUS:
				switch (to) {
				case CELSIUS:
					return value;
				case KELVIN:
					return value + 273.15;
				case FAHRENHEIT:
					return value * 9.0 / 5.0 + 32;
				}
			case KELVIN:
				switch (to) {
				case CELSIUS:
					return value - 273.15;
				case KELVIN:
					return value;
				case FAHRENHEIT:
					return (value - 273.15 - 32) * 5.0 / 9.0;
				}
			case FAHRENHEIT:
				switch (to) {
				case CELSIUS:
					return (value - 32) * 5.0 / 9.0;
				case KELVIN:
					return (value - 32) * 5.0 / 9.0 + 273.15;
				case FAHRENHEIT:
					return value;
				}
			}
			return null; // never reached
		}
	}
}
