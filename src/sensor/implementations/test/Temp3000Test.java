package sensor.implementations.test;

import java.rmi.RemoteException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import sensor.TempSensor;
import sensor.TempSensor.Unit;
import sensor.implementations.Temp3000;

public class Temp3000Test {

	TempSensor t;

	@Before
	public void setUp() throws Exception {
		t = new Temp3000();
	}

	@Test
	public void testReadTemperature() throws RemoteException {
		System.out.println("Sync " + t.readTemperature(Unit.CELSIUS));
		System.out.println("Sync " + t.readTemperature(Unit.CELSIUS));
		System.out.println("Sync " + t.readTemperature(Unit.CELSIUS));
	}

	@Test
	public void testReadTemperatureAsync() throws RemoteException, InterruptedException, ExecutionException {
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
