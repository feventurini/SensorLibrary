package sensor.implementations.test;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;

import discovery.Provider;
import discovery.ProviderRMI;
import sensor.FutureResult;
import sensor.TempSensor;
import sensor.TempSensor.Unit;
import sensor.implementations.Temp2000;

// NON VA
public class RmiTemp2000Test {
	
	public static void main(String[] args) throws Exception {
		LocateRegistry.createRegistry(1099);
		ProviderRMI.main(new String[] {}); // avvio provider
		Provider p = (Provider) Naming.lookup("//localhost:1099/ProviderRMI");
		p.register("test", "Temp2000", new Temp2000());
		TempSensor t = (TempSensor) p.find("test", "Temp2000");
		System.out.println("Set up ok");

		System.out.println("Sync " + t.readTemperature(Unit.CELSIUS));
		System.out.println("Sync " + t.readTemperature(Unit.CELSIUS));
		System.out.println("Sync " + t.readTemperature(Unit.CELSIUS));
	
		List<FutureResult<Double>> results = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			results.add(t.readTemperatureAsync(Unit.CELSIUS));
		}
		results.forEach((future) -> {
			new Thread(() -> {
				try {
					System.out.println("Async " + future.get());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();
		});
		Thread.sleep(10000);
	}
}
