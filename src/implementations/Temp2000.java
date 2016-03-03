package implementations;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import http.IpUtils;
import http.RmiClassServer;
import provider.Provider;
import sensor.FutureResult;
import sensor.FutureResultImpl;
import sensor.SensorParameter;
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
		if (isSetUp == false)
			throw new IllegalStateException("Sensor setup incomplete");
		try {
			Thread.sleep(r.nextInt(5000) + 1000);
		} catch (InterruptedException e) {
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

	@Override
	public void setUp() {
		if (!allParametersFilledUp()) {
			isSetUp = false;
		} else {
			sensorUnit = Unit.KELVIN;
			r = new Random();
			executor = Executors.newFixedThreadPool(1);
			isSetUp = true;
		}
	}

	@Override
	public void tearDown() {
		executor.shutdown();
		System.out.println("Temp2000 stopped");
	}

	@Override
	public String getState() {
		StringBuilder sb = new StringBuilder();
		sb.append("Sensor " + this.getClass().getSimpleName() + "\n");
		if (isSetUp) {
			sb.append("\tstate: running\n");
			sb.append("\tunit of measure:" + sensorUnit + "\n");
			sb.append("\tdelay between measures:" + delay + "\n");
		} else {
			sb.append("\tstate: not set up\n");
		}
		return sb.toString();
	}
}
