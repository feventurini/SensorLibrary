

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import discovery.Provider;
import sensor.FutureResult;
import sensor.RfidSensor;

public class TestUser {

	public static void main(String[] args) throws RemoteException, InterruptedException, MalformedURLException, NotBoundException{
		Provider p = (Provider) Naming.lookup("rmi://192.168.0.18:1099/ProviderRMI");
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
	}}
