package client;

import java.io.IOException;
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
	
	private Provider p;

	public TestUser(String[] args) throws Exception{
		if (args.length > 2) {
			System.out.println("Usage: TestUser [providerHost [providerPort]]");
			System.exit(1);
		}

		// la riga di comando ha precedenza sulla ricerca in multicast
		String providerUrl = null;
		if (args.length == 0) {
			try {
				providerUrl = Provider.findProviderUrl();
			} catch (IOException e) {
				System.out.println("Unable to get the provider address");
				System.exit(1);
			}
		}
		if (args.length == 1)
			providerUrl = Provider.buildProviderUrl(args[0]);
		if (args.length == 2) {
			try {
				providerUrl = Provider.buildProviderUrl(args[0], Integer.parseInt(args[1]));
			} catch (Exception e) {
				System.out.println("Unable to parse the provider address from the command line");
				System.exit(2);
			}
		}

		// Impostazione del SecurityManager
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		p = (Provider) Naming.lookup(providerUrl);
		System.out.println("Provider trovato");
	}

	public void provaRfid(String[] args)
			throws RemoteException, InterruptedException, MalformedURLException, NotBoundException {
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

	public void provaTemp(String[] args)
			throws Exception {
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

	public static void main(String[] args)
			throws Exception {
		new TestUser(args);
	}
}