package sensor.implementations.test;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;

import com.pi4j.io.gpio.RaspiPin;

import discovery.Provider;
import discovery.ProviderRMI;
import sensor.FutureResult;
import sensor.RfidSensor;
import sensor.implementations.Rfid_SL030;

// NON VA
public class RmiRfid_SL030Test {
	
	public static void main(String[] args) throws Exception {
		ProviderRMI.main(new String[] {"1099"}); // avvio provider
		Provider p = (Provider) Naming.lookup("//localhost:1099/ProviderRMI");
		p.register("test", "Rfid_SL030", new Rfid_SL030(RaspiPin.GPIO_07));
		RfidSensor t = (RfidSensor) p.find("test", "Rfid_SL030");
		System.out.println("Set up ok");

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
}
