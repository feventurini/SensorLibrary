package sensor.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import sensor.mocks.Temp4000;

public class Temp4000Test {

	Temp4000 t;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		t = new Temp4000();
		assertEquals(t.getState(), SensorState.SHUTDOWN);
		assertFalse(t.allParametersFilledUp());

		Properties p = new Properties();
		p.setProperty("InvalidateResultAfter", "5");
		t.loadParameters(p);
		assertTrue(t.allParametersFilledUp());
		assertEquals(t.invalidateResultAfter, (Long) 5L);

		t.customSetUp();
		assertEquals(t.getState(), SensorState.RUNNING);
	}

	/**
	 * Test method for {@link sensor.mocks.Temp4000#customTearDown()}.
	 */
	@After
	public void tearDown() throws RemoteException {
		assertEquals(t.getState(), SensorState.RUNNING);
		t.customTearDown();
		assertEquals(t.getState(), SensorState.SHUTDOWN);
	}

	/**
	 * Test method for
	 * {@link sensor.mocks.Temp4000#readTemperature()}
	 * .
	 */
	@Test
	public void testReadTemperature() throws RemoteException {
		assertNotNull(t.readTemperature());
	}

	/**
	 * Test method for
	 * {@link sensor.mocks.Temp4000#readTemperatureAsync()}
	 * .
	 */
	@Test
	public void testReadTemperatureAsync() throws RemoteException {
		FutureResult<Double> r = t.readTemperatureAsync();
		assertNotNull(r);
		assertNotNull(r.get());
	}
}
