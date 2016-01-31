package etc;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import discovery.Provider;
import discovery.ProviderRMI;
import sensor.TempSensor;
import sensor.TempSensor.Unit;

// NON VA
public class RmiTemp3000Test {

	TempSensor t;

	@Before
	public void setUp() throws Exception {
		ProviderRMI.main(new String[] {"1099"}); // avvio provider
		Provider p = (Provider) Naming.lookup("//localhost:1099/ProviderRMI");
		p.register("test", "Temp3000", new Temp3000());
		t = (TempSensor) p.find("test", "Temp3000");
	}

	@Test
	public void testReadTemperature() throws RemoteException {
		Assert.assertNotNull(t);
		System.out.println("Sync " + t.readTemperature(Unit.CELSIUS));
		System.out.println("Sync " + t.readTemperature(Unit.CELSIUS));
		System.out.println("Sync " + t.readTemperature(Unit.CELSIUS));
	}

	//@Test
	public void testReadTemperatureAsync() throws RemoteException, InterruptedException, ExecutionException {
		Assert.assertNotNull(t);
		for (int i = 0; i < 10; i++) {
			((CompletableFuture<Double>) t.readTemperatureAsync(Unit.CELSIUS))
					.thenAccept((value) -> System.out.println("Async " + value));
		}
	}
	
	@After
	public void tearDown() throws Exception {
		Thread.sleep(10000);
	}
}
