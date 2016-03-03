package client;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import provider.Provider;
import sensor.FutureResult;
import sensor.RfidSensor;
import sensor.TempSensor;
import sensor.TempSensor.Unit;

public class TestUser {

	public static void provaRfid(String[] args)
			throws RemoteException, InterruptedException, MalformedURLException, NotBoundException {
		int providerPort = 1099;
		String serviceName = "ProviderRMI";

		if (args.length < 1 || args.length > 2) {
			System.out.println("Usage: Client providerHost [providerPort]");
			System.exit(1);
		}

		String providerHost = args[0];
		if (args.length == 2) {
			try {
				providerPort = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.out.println("Usage: Client providerHost [providerPort]");
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

		String completeName = "rmi://" + providerHost + ":" + providerPort + "/" + serviceName;
		Provider p = (Provider) Naming.lookup(completeName);

		// Ricerca e uso del sensore
		RfidSensor t = (RfidSensor) p.find("cucina", "rfid");
		System.out.println("Set up ok, inizio misure");

		System.out.println("Sync " + t.readTag());
		System.out.println("SINCRONO");
		System.out.println("Sync " + t.readTag());
		System.out.println("SINCRONO");
		System.out.println("Sync " + t.readTag());
		System.out.println("SINCRONO");

		List<FutureResult<String>> results = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			results.add(t.readTagAsync());
			System.out.println("ASINCRONO SHALALALALALA");
		}
		results.forEach((futureResult) -> {
			new Thread(() -> {
				try {
					System.out.println("Async " + futureResult.get());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();
		});
		Thread.sleep(10000);
	}

	public static void provaTemp(String[] args)
			throws RemoteException, InterruptedException, MalformedURLException, NotBoundException {
		int providerPort = 1099;
		String serviceName = "ProviderRMI";

		if (args.length < 1 || args.length > 2) {
			System.out.println("Usage: Client providerHost [providerPort]");
			System.exit(1);
		}

		String providerHost = args[0];
		if (args.length == 2) {
			try {
				providerPort = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.out.println("Usage: Client providerHost [providerPort]");
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

		String completeName = "rmi://" + providerHost + ":" + providerPort + "/" + serviceName;
		Provider p = (Provider) Naming.lookup(completeName);

		// Ricerca e uso del sensore
		TempSensor t = (TempSensor) p.find("test_room", "Temp2000");
		System.out.println("Set up ok");

		System.out.println("Sync " + t.readTemperature(Unit.CELSIUS));
		System.out.println("SINCRONO");
		System.out.println("Sync " + t.readTemperature(Unit.CELSIUS));
		System.out.println("SINCRONO");
		System.out.println("Sync " + t.readTemperature(Unit.CELSIUS));
		System.out.println("SINCRONO");

		List<FutureResult<Double>> results = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			results.add(t.readTemperatureAsync(Unit.CELSIUS));
			System.out.println("ASINCRONO SHALALALALALA");
		}
		results.forEach((futureResult) -> {
			new Thread(() -> {
				try {
					System.out.println("Async " + futureResult.get());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();
		});
		Thread.sleep(10000);
	}
	
	public static void main(String[] args) throws RemoteException, MalformedURLException, InterruptedException, NotBoundException {
		provaRfid(args);
	}
}