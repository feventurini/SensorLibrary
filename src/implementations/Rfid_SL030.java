package implementations;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.bind.DatatypeConverter;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

import sensor.FutureResult;
import sensor.FutureResultImpl;
import sensor.RfidSensor;
import sensor.SensorParameter;
import sensor.SensorState.State;

public class Rfid_SL030 extends SensorServer implements RfidSensor {
	private static final long serialVersionUID = 4894977624049261879L;

	// RFID addresses
	private static final int DEVICE_ADDRESS = 0x50;
	// Device object
	private static I2CDevice sl030;
	// I2C bus
	private I2CBus bus;
	private GpioPinDigitalInput trigger;
	private int errorCounter;

	private final int errorTreshold = 20;

	@SensorParameter(userDescription = "Trigger pin", propertyName = "trigger")
	public Integer triggerPinNumber;

	private ConcurrentLinkedQueue<FutureResultImpl<String>> queue;

	public Rfid_SL030() throws RemoteException {
		queue = new ConcurrentLinkedQueue<>();
		errorCounter = 0;

	}

	public String readRfid() throws Exception {

		switch (state.getState()) {
		case SETUP:
			throw new IllegalStateException("Sensor setup incomplete");
		case FAULT:
			throw new IllegalStateException("Sensor fault");
		case SHUTDOWN:
			throw new IllegalStateException("Sensor shutdown");
		default:
			break;
		}

		byte[] writeBuffer = new byte[2];
		try {
			// SEND COMMAND TO DEVICE TO REQUEST READING A MIFARE CARD
			// | LENGTH | CMD |
			writeBuffer[0] = (byte) 0x01; // LENGTH (CMD+DATA)
			writeBuffer[1] = (byte) 0x01; // CMD: SELECT MIFARE CARD
			sl030.write(writeBuffer, 0, writeBuffer.length);

			Thread.sleep(1000);

			// READ SELECT-TAG RESPONSE
			// | LENGTH | STATUS | UID (4-7 byte) | TYPE
			// first just read in a single byte that represents the
			// command+status+data payload length
			int length = sl030.read();
			System.out.println("TOTAL BYTES AVAILABLE: " + length);

			// if there are no remaining bytes (length == 0), then we can exit
			// the function
			if (length <= 0) {
				System.out.println("Error: " + length + " bytes read for LENGTH");
				return "NO-TAG";
			}

			// if there is any length of remaining bytes, then lets read them
			// now
			byte[] readBuffer = new byte[length + 1];
			int readTotal = sl030.read(readBuffer, 0, length);
			System.out.println("Letti: " + readTotal);
			// validate to ensure we got back at least the command and status
			// bytes
			if (readTotal <= 3) {
				System.out.println("Error: only " + readTotal + " bytes read");
				return "NO-TAG";
			}

			byte[] uidByte = new byte[readBuffer.length - 4];
			for (int i = 0; i < uidByte.length; i++) {
				uidByte[i] = readBuffer[i + 3];
			}

			// now we need to get the payload data (if there is any?)
			String uid = DatatypeConverter.printHexBinary(uidByte);

			System.out.println("Tag read: " + uid);
			errorCounter = 0; // reset error counter
			return uid.trim();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			System.out.println("Error: " + e.getMessage());
			errorCounter++;
			if (errorCounter >= errorTreshold) {
				state.setState(State.FAULT);
				throw e;
			}
			return "NO-TAG";
		}
	}

	@Override
	public String readTag() throws RemoteException {
		return readTagAsync().get();
	}

	@Override
	public FutureResult<String> readTagAsync() throws RemoteException {
		FutureResultImpl<String> result = new FutureResultImpl<>();
		queue.add(result);
		return result;
	}

	@Override
	public void setUp() throws Exception {

		if (!allParametersFilledUp()) {
			state.setState(State.SETUP);
		} else {
			final GpioController gpio = GpioFactory.getInstance();
			// provision gpio pin as an input pin with its internal pull
			// down
			// resistor enabled
			Pin triggerPin = RaspiPin.getPinByName("GPIO " + triggerPinNumber);
			trigger = gpio.provisionDigitalInputPin(triggerPin, PinPullResistance.PULL_DOWN);
			trigger.setDebounce(1000);
			// get the bus
			bus = I2CFactory.getInstance(I2CBus.BUS_1);
			System.out.println("Connected to bus");
			// get device itself
			sl030 = bus.getDevice(DEVICE_ADDRESS);
			System.out.println("Connected to device");

			trigger.addListener(new GpioPinListenerDigital() {
				@Override
				public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
					if (!queue.isEmpty() && event.getState() == PinState.LOW) {
						// something to read
						String rfid = null;
						try {
							rfid = readRfid();
							System.out.println("Letto RFID " + rfid);
							if (!rfid.equalsIgnoreCase("NO-TAG")) {
								while (!queue.isEmpty()) {
									queue.poll().set(rfid);
								}
							}

						} catch (Exception e) {
							e.printStackTrace();
							queue.poll().raiseException(e);
						}

					}
				}
			});
			state.setState(State.RUNNING);
		}

	}

	@Override
	public void tearDown() {
		super.tearDown();
		if (trigger != null) {
			trigger.removeAllListeners();
		}
		GpioFactory.getInstance().unprovisionPin(trigger);
	}
}
