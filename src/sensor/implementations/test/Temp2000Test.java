package sensor.implementations.test;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import sensor.TempSensor;
import sensor.TempSensor.Unit;
import sensor.implementations.Temp2000;

public class Temp2000Test {

	TempSensor t;

	@Before
	public void setUp() throws Exception {
		t = new Temp2000();
	}

	@Test
	public void testReadTemperature() throws RemoteException {
		System.out.println("Sync " + t.readTemperature(Unit.CELSIUS));
		System.out.println("Sync " + t.readTemperature(Unit.CELSIUS));
		System.out.println("Sync " + t.readTemperature(Unit.CELSIUS));
	}

	@Test
	public void testReadTemperatureAsync() throws RemoteException, InterruptedException, ExecutionException {
		List<Future<Double>> results = new ArrayList<>();
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
	}

	@After
	public void tearDown() throws Exception {
		Thread.sleep(10000);
	}
}
