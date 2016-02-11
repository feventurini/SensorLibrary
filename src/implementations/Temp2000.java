package implementations;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sensor.FutureResult;
import sensor.FutureResultImpl;
import sensor.TempSensor;

public class Temp2000 extends UnicastRemoteObject implements TempSensor {
	private static final long serialVersionUID = -9066863232278842877L;
	private Random r;
	private ExecutorService executor;
	private Unit sensorUnit;

	public Temp2000() throws RemoteException {
		super();
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

	// java -Djava.security.policy=rmi.policy implementations.Temp2000
	// 192.168.0.12
}
