package client;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import provider.Provider;
import sensor.FutureResult;
import sensor.RfidSensor;
import sensor.TempSensor;
import sensor.TempSensor.Unit;

public class TestUser {

	private Provider p;

	public TestUser(String[] args) throws Exception {
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
				System.out.println("Unable to get the provider address using multicast");
				if (e instanceof BindException)
					System.out.println(
							"Got BindException, maybe the provider is on this machine and has already taken 230.0.0.1");
			}
		} else if (args.length == 1) {
			providerUrl = Provider.buildProviderUrl(args[0]);
		} else if (args.length == 2) {
			try {
				providerUrl = Provider.buildProviderUrl(args[0], Integer.parseInt(args[1]));
			} catch (Exception e) {
				System.out.println("Unable to parse the provider address from the command line");
				System.exit(2);
			}
		}

		// Impostazione del SecurityManager
		if (!new File("rmi.policy").canRead()) {
			System.out.println(
					"Unable to load security policy, assure that you have rmi.policy in the directory you launched ProviderRMI in");
			System.exit(-3);
		}
		System.setProperty("java.security.policy", "rmi.policy");
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		p = (Provider) Naming.lookup(providerUrl);
		System.out.println("Provider trovato");

		provaTemp();
		// provaRfid();
	}

	public void provaRfid() throws RemoteException, InterruptedException, MalformedURLException, NotBoundException {
		// Ricerca e uso del sensore
		RfidSensor t = (RfidSensor) p.find("camera", "rfid");
		System.out.println("Trovato sensore, inizio misure");

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

	public void provaTemp() throws Exception {
		// Ricerca e uso del sensore
		TempSensor t = (TempSensor) p.find("camera", "temp");
		System.out.println("Trovato sensore, inizio misure");

		System.out.println(
				"Mando 3 richieste sincrone e aspetto (la prima ci mette un po' perchè deve misurare, le altre due sono immediate perchè la misura è ancora fresca)");
		System.out.print("Sync ");
		System.out.println(t.readTemperature(Unit.CELSIUS));
		System.out.print("Sync ");
		System.out.println(t.readTemperature(Unit.CELSIUS));
		System.out.print("Sync ");
		System.out.println(t.readTemperature(Unit.CELSIUS));

		System.out.println("Mando 10 richieste asincrone e faccio altro");
		List<FutureResult<Double>> results = new ArrayList<>();
		for (int i = 0; i < 10; i++)
			results.add(t.readTemperatureAsync(Unit.CELSIUS));

		System.out.println("Per ogni future result faccio partire un thread che quando ha il risultato lo stampa"); 
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

	public static void main(String[] args) throws Exception {
		new TestUser(args);
	}
}