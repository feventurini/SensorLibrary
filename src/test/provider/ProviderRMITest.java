package provider;

import static org.junit.Assert.fail;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import sensor.base.Sensor;

public class ProviderRMITest {

	Provider p;
	ArrayList<String> sensorIds;

	@BeforeClass
	public static void createProvider() throws Exception {
		// remove old registry if present
		try {
			LocateRegistry.getRegistry();
			UnicastRemoteObject.unexportObject(LocateRegistry.getRegistry(), true);
		} catch (RemoteException e) {
		}
		ProviderRMI.main(new String[0]);
	}

	@Before
	public void setUp() throws Exception {
		p = (Provider) Naming.lookup(ProviderUtils.findProviderUrl());
		sensorIds = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			sensorIds.add(UUID.randomUUID().toString());
		}
		sensorIds.stream().forEach((name) -> {
			try {
				p.register(new SensorId(name, "mocklocation"), Mockito.mock(Sensor.class));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}});
	}

	@AfterClass
	public static void tearDown() throws Exception {
		// remove registry if present
		try {
			LocateRegistry.getRegistry();
			UnicastRemoteObject.unexportObject(LocateRegistry.getRegistry(), true);
		} catch (RemoteException e) {
		}
	}

	@Test
	public void testFind() {
		fail("Not yet implemented");
	}

	@Test
	public void testFindAll() {
		fail("Not yet implemented");
	}

	@Test
	public void testRegister() {
		fail("Not yet implemented");
	}

	@Test
	public void testRegisterStation() {
		fail("Not yet implemented");
	}

	@Test
	public void testUnregister() {
		fail("Not yet implemented");
	}

	@Test
	public void testUnregisterStation() {
		fail("Not yet implemented");
	}

}
