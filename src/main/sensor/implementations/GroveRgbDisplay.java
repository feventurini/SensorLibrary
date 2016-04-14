package sensor.implementations;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.iot.raspberry.grovepi.devices.GroveRgbLcd;
import org.iot.raspberry.grovepi.pi4j.GroveRgbLcdPi4J;

import sensor.base.SensorServer;
import sensor.interfaces.RgbLcdDisplay;

public class GroveRgbDisplay extends SensorServer implements RgbLcdDisplay {
	private static final long serialVersionUID = 682226100883364068L;
	private final static Logger log = Logger.getLogger(GroveRgbDisplay.class.getName());

	private GroveRgbLcd display;
	private ScheduledThreadPoolExecutor executor;

	private Runnable clearer = () -> {
		try {
			display.setText("");
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error during write to sensor", e);
			fail();
		}
	};

	public GroveRgbDisplay() throws RemoteException {
		super();

	}

	@Override
	public synchronized void display(String text, int time) throws RemoteException {
		switch (getState()) {
		case FAULT:
			throw new IllegalStateException("Sensor fault");
		case SHUTDOWN:
			throw new IllegalStateException("Sensor shutdown");
		default:
			try {
				executor.shutdownNow();
				executor = new ScheduledThreadPoolExecutor(1); // sicuramente
																// c'è un modo
																// migliore
				// executor.remove(clearer);
				// if someone was waiting to clear the
				// display, prevents the thread to act
				display.setText(text);
				if (time != 0) {
					executor.schedule(clearer, time, TimeUnit.SECONDS);
				}
			} catch (IOException e) {
				log.log(Level.SEVERE, "Error writing to the display", e);
				fail();
			}
		}
	}

	@Override
	public synchronized void setRGB(int r, int g, int b) throws RemoteException {
		switch (getState()) {
		case FAULT:
			throw new IllegalStateException("Sensor fault");
		case SHUTDOWN:
			throw new IllegalStateException("Sensor shutdown");
		default:

			try {
				display.setRGB(r, g, b);
			} catch (IOException e) {
				log.log(Level.SEVERE, "Error writing to the display", e);
				fail();
			}
		}
	}

	@Override
	public void customSetUp() throws Exception {
		try {
			display = new GroveRgbLcdPi4J();
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error writing to the display", e);
			throw e;
		}
		executor = new ScheduledThreadPoolExecutor(1);
	}

	@Override
	public void customTearDown() {
		try {
			display.setRGB(0, 0, 0);
			display.setText("");
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error writing to the display", e);
		}
	}

	@Override
	protected void customFail() {
		// TODO Auto-generated method stub
		
	}

}
