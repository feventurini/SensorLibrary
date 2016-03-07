package implementations;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.iot.raspberry.grovepi.devices.GroveRgbLcd;
import org.iot.raspberry.grovepi.pi4j.GroveRgbLcdPi4J;

import sensor.RgbLcdDisplay;
import sensor.SensorState.State;

public class GroveRgbDisplay extends SensorServer implements RgbLcdDisplay {
	private static final long serialVersionUID = 682226100883364068L;

	private GroveRgbLcd display;
	private ScheduledThreadPoolExecutor executor;

	private Runnable clearer = () -> {
		try {
			display.setText("");
		} catch (IOException e) {
			state.setState(State.FAULT);
			e.printStackTrace();
		}
	};

	public GroveRgbDisplay() throws RemoteException {
		super();

	}

	@Override
	public synchronized void display(String text, int time) throws RemoteException {
		switch (state.getState()) {
		case SETUP:
			throw new IllegalStateException("Sensor setup incomplete");
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
				// executor.remove(clearer); // if someone was waiting to clear
				// the
				// display, prevents the thread to
				// act
				display.setText(text);
				if (time != 0) {
					executor.schedule(clearer, time, TimeUnit.SECONDS);
				}
			} catch (IOException e) {
				state.setState(State.FAULT);
				e.printStackTrace();
			}
		}
	}

	@Override
	public synchronized void setRGB(int r, int g, int b) throws RemoteException {
		switch (state.getState()) {
		case SETUP:
			throw new IllegalStateException("Sensor setup incomplete");
		case FAULT:
			throw new IllegalStateException("Sensor fault");
		case SHUTDOWN:
			throw new IllegalStateException("Sensor shutdown");
		default:

			try {
				display.setRGB(r, g, b);
			} catch (IOException e) {
				state.setState(State.FAULT);
				e.printStackTrace();
			}
		}
	}

	@Override
	public void setUp() {
		if (!allParametersFilledUp()) {
			state.setState(State.SETUP);
			state.setComment("Set up");
		} else {
			try {
				display = new GroveRgbLcdPi4J();
			} catch (IOException e) {
				state.setState(State.FAULT);
			}
			executor = new ScheduledThreadPoolExecutor(1);
			state.setState(State.RUNNING);
			state.setComment("Running");
		}
	}

	@Override
	public void tearDown() {
		try {
			display.setRGB(0, 0, 0);
			display.setText("");
		} catch (IOException e) {
			e.printStackTrace();
		}
		state.setState(State.SHUTDOWN);
		state.setComment("Shutdown");
		System.out.println("GroveRGBDisplay stopped");
	}

}
