package client;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;

import provider.Provider;
import provider.ProviderUtils;
import sensor.base.FutureResult;
import sensor.base.Sensor;
import sensor.interfaces.RfidSensor;
import sensor.interfaces.RgbLcdDisplay;
import sensor.interfaces.TempSensor;
import sensor.interfaces.TempSensor.Unit;

public class TestUser {

	public static void main(String[] args) throws Exception {
		new TestUser(args);
		System.exit(0);
	}

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
				providerUrl = ProviderUtils.findProviderUrl();
			} catch (IOException e) {
				System.out.println("Unable to get the provider address using multicast");
			}
		} else if (args.length == 1) {
			providerUrl = ProviderUtils.buildProviderUrl(args[0]);
		} else if (args.length == 2) {
			try {
				providerUrl = ProviderUtils.buildProviderUrl(args[0], Integer.parseInt(args[1]));
			} catch (Exception e) {
				System.out.println("Unable to parse the provider address from the command line");
				System.exit(2);
			}
		}

		// Impostazione del SecurityManager
		if (!new File("assets/rmi.policy").canRead()) {
			System.out
					.println("Unable to load security policy, assure that you have rmi.policy in the assets directory");
			System.exit(-3);
		}
		System.setProperty("java.security.policy", "assets/rmi.policy");
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		p = (Provider) Naming.lookup(providerUrl);
		System.out.println("Provider trovato");

		provaReflection();
		provaTemp();
		provaRfid();
	}

	private void provaReflection() {
		// https://code.google.com/archive/p/reflections/
		Reflections reflections = new Reflections("");
		Set<Class<? extends Sensor>> subTypes = reflections.getSubTypesOf(Sensor.class);

		subTypes.stream().sorted((o1, o2) -> o1.getSimpleName().compareTo(o2.getSimpleName()))
				.filter((interfaccia) -> interfaccia.isInterface()).forEach((interfaccia) -> {
					StringBuilder sb = new StringBuilder();
					sb.append(interfaccia.getSimpleName()).append("\n");
					for (Method m : interfaccia.getMethods()) {
						sb.append("\t").append(m.getReturnType().getSimpleName()).append(" ").append(m.getName())
								.append("(");
						if (m.getParameterCount() == 0) {
							sb.append(")\n");
						} else {
							for (int i = 0; i < m.getParameterCount(); i++) {
								sb.append(m.getParameterTypes()[i].getSimpleName()).append(" ")
										.append(m.getParameters()[i].getName())
										.append(i == m.getParameterCount() - 1 ? ")\n" : ", ");
							}
						}
					}
					System.out.println(sb.toString());
				});
	}

	public void provaRfid() throws RemoteException, InterruptedException, MalformedURLException, NotBoundException {
		// Ricerca e uso del sensore
		RfidSensor t = (RfidSensor) p.find("rfidFrigo", "cucina");
		RgbLcdDisplay d = (RgbLcdDisplay) p.find("display", "cucina");
		System.out.println("Trovato sensore, inizio misure");

		String tag = t.readTag();
		System.out.println("Sync " + tag);
		System.out.println("SINCRONO");
		d.setRGB(0, 0, 255);
		d.display(tag, 5);

		tag = t.readTag();
		System.out.println("Sync " + tag);
		System.out.println("SINCRONO");
		d.setRGB(0, 255, 0);
		d.display(tag, 6);

		tag = t.readTag();
		System.out.println("Sync " + tag);
		System.out.println("SINCRONO");
		d.setRGB(138, 43, 226);
		d.display(tag, 10);

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
		d.setRGB(0, 0, 0);
		d.display("", 0);
	}

	public void provaTemp() throws Exception {
		// Ricerca e uso del sensore
		TempSensor t = (TempSensor) p.find("tempAmbiente", "cucina");
		RgbLcdDisplay d = (RgbLcdDisplay) p.find("display", "cucina");
		System.out.println("Trovato sensore, inizio misure");

		double temp;

		System.out.println(
				"Mando 3 richieste sincrone e aspetto (la prima ci mette un po' perchè deve misurare, le altre due sono immediate perchè la misura è ancora fresca)");
		System.out.print("Sync ");
		temp = t.readTemperature(Unit.CELSIUS);
		System.out.println(temp);
		d.setRGB(255, 0, 0);
		d.display("" + temp, 5);
		Thread.sleep(2000);

		System.out.print("Sync ");
		temp = t.readTemperature(Unit.CELSIUS);
		System.out.println(temp);
		d.setRGB(0, 255, 0);
		d.display("" + temp, 5);

		Thread.sleep(10000);
		System.out.print("Sync ");
		temp = t.readTemperature(Unit.CELSIUS);
		System.out.println(temp);
		d.setRGB(0, 0, 255);
		d.display("" + temp, 10);

		System.out.println("Mando 10 richieste asincrone e faccio altro");
		List<FutureResult<Double>> results = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			results.add(t.readTemperatureAsync(Unit.CELSIUS));
		}

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
		d.setRGB(0, 0, 0);
		d.display("", 0);
	}
}