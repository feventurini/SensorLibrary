package sensor.implementations;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import sensor.base.FutureResult;
import sensor.base.FutureResultImpl;
import sensor.base.SensorParameter;
import sensor.base.SensorServer;
import sensor.base.SensorState;
import sensor.interfaces.RfidSensor;

public class Rfid_SL030 extends SensorServer implements RfidSensor {
	private static final long serialVersionUID = 4894977624049261879L;
	private final static Logger log = Logger.getLogger(Rfid_SL030.class.getName());

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
	}

	public String readRfid() throws Exception {

		switch (getState()) {
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
			log.info("TOTAL BYTES AVAILABLE: " + length);

			// if there are no remaining bytes (length == 0), then we can exit
			// the function
			if (length <= 0) {
				log.severe("Error: " + length + " bytes read for LENGTH");
				return "NO-TAG";
			}

			// if there is any length of remaining bytes, then lets read them
			// now
			byte[] readBuffer = new byte[length + 1];
			int readTotal = sl030.read(readBuffer, 0, length);
			log.info("Letti: " + readTotal);
			// validate to ensure we got back at least the command and status
			// bytes
			if (readTotal <= 3) {
				log.warning("Error: only " + readTotal + " bytes read");
				return "NO-TAG";
			}

			byte[] uidByte = new byte[readBuffer.length - 4];
			for (int i = 0; i < uidByte.length; i++) {
				uidByte[i] = readBuffer[i + 3];
			}

			// now we need to get the payload data (if there is any?)
			String uid = DatatypeConverter.printHexBinary(uidByte);

			log.info("Tag read: " + uid);
			errorCounter = 0; // reset error counter
			return uid.trim();
		} catch (IOException | InterruptedException e) {
			log.log(Level.WARNING, "Error reading from the sensor", e);
			errorCounter++;
			if (errorCounter >= errorTreshold) {
				setState(SensorState.FAULT);
				log.severe("Rfid_SL030 state set to FAULT because of repeated failures");
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
	public void customSetUp() throws Exception {
		final GpioController gpio = GpioFactory.getInstance();
		// provision gpio pin as an input pin with its internal pull
		// down
		// resistor enabled
		Pin triggerPin = RaspiPin.getPinByName("GPIO " + triggerPinNumber);
		trigger = gpio.provisionDigitalInputPin(triggerPin, PinPullResistance.PULL_DOWN);
		trigger.setDebounce(1000);
		// get the bus
		bus = I2CFactory.getInstance(I2CBus.BUS_1);
		log.info("Connected to bus");
		// get device itself
		sl030 = bus.getDevice(DEVICE_ADDRESS);
		log.info("Connected to device");

		trigger.addListener(new GpioPinListenerDigital() {
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
				if (!queue.isEmpty() && event.getState() == PinState.LOW) {
					// something to read
					String rfid = null;
					try {
						rfid = readRfid();
						log.info("Letto RFID " + rfid);
						if (!rfid.equalsIgnoreCase("NO-TAG")) {
							while (!queue.isEmpty()) {
								queue.poll().set(rfid);
							}
						}
					} catch (Exception e) {
						queue.poll().raiseException(e);
						e.printStackTrace();
					}

				}
			}
		});
		errorCounter = 0;
		setState(SensorState.RUNNING);
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
