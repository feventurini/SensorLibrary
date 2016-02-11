package client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.server.RMIClassLoader;
import java.util.ArrayList;
import java.util.List;

import http.RmiClassServer;
import provider.Provider;
import sensor.FutureResult;
import sensor.TempSensor;
import sensor.TempSensor.Unit;

// NON VA
public class RmiTemp2000Test {

	public static void main(String[] args) {
		int providerPort = 1099;
		String serviceName = "ProviderRMI";

		if (args.length > 2) {
			System.out.println("Usage: RmiTemp2000Test [providerHost [providerPort]]");
			System.exit(1);
		}

		String providerHost = null;
		if (args.length>0) {
			providerHost = args[0];
		} else {
			try {
				providerHost = Provider.findProviderHost();
			} catch (IOException e) {
				System.out.println("Unable to get the provider address");
				System.exit(1);
			}
		}
		
		if (args.length == 2) {
			try {
				providerPort = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.out.println("Usage: RmiTemp2000Test [providerHost [providerPort]]");
				System.exit(2);
			}
			if (providerPort < 1024 || providerPort > 65535) {
				System.out.println("Port out of range");
				System.exit(3);
			}
		}

		// Impostazione del SecurityManager
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}

		Provider p = null;
		try {
			String completeName = "rmi://" + providerHost + ":" + providerPort + "/" + serviceName;
			p = (Provider) Naming.lookup(completeName);
			System.out.println("Provider trovato, annotazione " + RMIClassLoader.getClassAnnotation(p.getClass()));
		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.exit(4);
		} catch (RemoteException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(4);
		} catch (NotBoundException e) {
			System.out.println("Unable to find the provider at "+providerHost+":"+providerPort);
			e.printStackTrace();
			System.exit(4);
		}
		
		TempSensor t = null;
		try {
			t = (TempSensor) p.find("test_room", "Temp2000");
			System.out.println("Sensore trovato, annotazione " + RMIClassLoader.getClassAnnotation(t.getClass()));
		} catch (RemoteException e) {
			System.out.println("Sensore non trovato");
			System.exit(4);
		}

		try {
			System.out.println("Sync " + t.readTemperature(Unit.CELSIUS));
			System.out.println("SINCRONO");
			System.out.println("Sync " + t.readTemperature(Unit.CELSIUS));
			System.out.println("SINCRONO");

			List<FutureResult<Double>> results = new ArrayList<>();
			for (int i = 0; i < 3; i++) {
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
		} catch (RemoteException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(5);
		}
	}
}
