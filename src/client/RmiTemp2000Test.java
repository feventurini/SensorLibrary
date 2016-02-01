package client;

import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.util.ArrayList;
import java.util.List;

import provider.Provider;
import sensor.FutureResult;
import sensor.TempSensor;
import sensor.TempSensor.Unit;

// NON VA
public class RmiTemp2000Test {

	public static void main(String[] args) throws Exception {
		int providerPort = 1099;
		String serviceName = "ProviderRMI";

		if (args.length < 1 || args.length > 2) {
			System.out.println("Usage: RmiTemp2000Test providerHost [providerPort]");
			System.exit(1);
		}

		String providerHost = args[0];
		if (args.length == 2) {
			try {
				providerPort = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.out.println("Usage: RmiTemp2000Test providerHost [providerPort]");
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

		String completeName = "rmi://" + providerHost + ":" + providerPort + "/" + serviceName;
		Provider p = (Provider) Naming.lookup(completeName);
		System.out.println("Provider trovato");
		
		TempSensor t = (TempSensor) p.find("test_room", "Temp2000");
		System.out.println("Sensore trovato");

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
}
