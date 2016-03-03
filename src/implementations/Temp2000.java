package implementations;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
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

	public Temp2000() throws RemoteException {
		this(Unit.KELVIN);
	}

	/**
	 * Creates a new Temp2000 using the specified unit
	 * 
	 * @param unit
	 * @throws RemoteException
	 */
	public Temp2000(Unit unit) throws RemoteException {
		super();
		sensorUnit = unit;
	}

	private Double measure(Unit unit) {
		if (isSetUp == false)
			throw new IllegalStateException("Sensor setup incomplete");
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

	@Override
	public void setUp() {
		if (!allParametersFilledUp()) {
			isSetUp = false;
		} else {
			r = new Random();
			executor = Executors.newFixedThreadPool(1);
			isSetUp = true;
		}
	}

	@Override
	public void tearDown() {
		// nothing to do here
		System.out.println("Temp2000 stopped");
	}

	// java -Djava.security.policy=rmi.policy implementations.Temp2000
	// 192.168.0.12
	public static void main(String[] args) {
		int providerPort = 1099;
		String serviceName = "ProviderRMI";

		if (args.length < 1 || args.length > 2) {
			System.out.println("Usage: Temp2000 providerHost [providerPort]");
			System.exit(1);
		}

		String providerHost = args[0];
		if (args.length == 2) {
			try {
				providerPort = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.out.println("Usage: Temp2000 providerHost [providerPort]");
				System.exit(2);
			}
			if (providerPort < 1024 || providerPort > 65535) {
				System.out.println("Port out of range");
				System.exit(3);
			}
		}

		// Impostazione del SecurityManager
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		// Avvia un server http affinchè altri possano scaricare gli stub di
		// questa classe
		// da questo codebase in maniera dinamica quando serve
		// (https://publicobject.com/2008/03/java-rmi-without-webserver.html)
		String currentHostname;
		try {
			currentHostname = IpUtils.getCurrentIp().getHostAddress();
			RmiClassServer rmiClassServer = new RmiClassServer();
			rmiClassServer.start();
			System.setProperty("java.rmi.server.hostname", currentHostname);
			System.setProperty("java.rmi.server.codebase",
					"http://" + currentHostname + ":" + rmiClassServer.getHttpPort() + "/");
		} catch (SocketException | UnknownHostException e) {
			System.out.println("Unable to get the local address");
			e.printStackTrace();
			System.exit(1);
		}

		try {
			// Ricerca del provider e registrazione
			String completeName = "rmi://" + providerHost + ":" + providerPort + "/" + serviceName;
			Provider p = (Provider) Naming.lookup(completeName);
			p.register("test_room", "Temp2000", new Temp2000());
			System.out.println("Temp2000 registrato");
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			System.out.println("Impossible to register sensor");
			e.printStackTrace();
		}
	}
}
