package implementations;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
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

import http.IpUtils;
import http.RmiClassServer;
import provider.Provider;
import sensor.FutureResult;
import sensor.FutureResultImpl;
import sensor.RfidSensor;
import sensor.SensorParameter;
import sensor.SensorState.State;

public class Rfid_SL030 extends SensorServer implements RfidSensor {
	private static final long serialVersionUID = 4894977624049261879L;

	// RFID addresses
	private static final int DEVICE_ADDRESS = 0x50;
	// I2C bus
	private I2CBus bus;
	private GpioPinDigitalInput trigger;

	@SensorParameter(userDescription = "Trigger pin", propertyName = "trigger")
	public Integer triggerPinNumber;

	private ConcurrentLinkedQueue<FutureResultImpl<String>> queue;

	// Device object
	private static I2CDevice sl030;

	public Rfid_SL030() throws RemoteException {
		queue = new ConcurrentLinkedQueue<>();

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
							if (!rfid.equalsIgnoreCase("NO-TAG"))
								while (!queue.isEmpty())
									queue.poll().set(rfid);

						} catch (IOException e) {
							e.printStackTrace();
						}

					}
				}
			});
			state.setState(State.RUNNING);
		}

	}

	@Override
	public void tearDown() {
		state.setState(State.SHUTDOWN);
		trigger.removeAllListeners();
	}

	public String readRfid() throws IOException {

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

			byte len = readBuffer[0];
			byte command = readBuffer[1]; // COMMAND BYTE
			byte status = readBuffer[2]; // STATUS BYTES
			byte type = readBuffer[readBuffer.length - 1];
			byte[] uidByte = new byte[readBuffer.length - 4];
			for (int i = 0; i < uidByte.length; i++) {
				uidByte[i] = readBuffer[i + 3];
			}

			// now we need to get the payload data (if there is any?)
			String uid = DatatypeConverter.printHexBinary(uidByte);

			System.out.println("Tag read: " + uid);
			return uid.trim();
		} catch (IOException | InterruptedException e) {
			System.out.println("Error: " + e.getMessage());
			state.setState(State.FAULT);
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

	// java -Djava.security.policy=rmi.policy implementations.Rfid_SL030
	// 192.168.0.12
	public static void main(String[] args) {
		int providerPort = 1099;
		String serviceName = "ProviderRMI";

		if (args.length < 1 || args.length > 2) {
			System.out.println("Usage: Rfid_SL030 providerHost [providerPort]");
			System.exit(1);
		}

		String providerHost = args[0];
		if (args.length == 2) {
			try {
				providerPort = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.out.println("Usage: Rfid_SL030 providerHost [providerPort]");
				System.exit(2);
			}
			if (providerPort < 1024 || providerPort > 65535) {
				System.out.println("Port out of range");
				System.exit(3);
			}
		}

		// Impostazione del SecurityManager
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		// Avvia un server http affinchè altri possano scaricare gli stub di
		// questa classe
		// da questo codebase in maniera dinamica quando serve
		// (https://publicobject.com/2008/03/java-rmi-without-webserver.html)
		String currentHostname;
		try {
			currentHostname = IpUtils.getCurrentIp().getHostAddress();
			RmiClassServer rmiClassServer = new RmiClassServer();
			rmiClassServer.start();
			System.setProperty("java.rmi.server.hostname", currentHostname);
			System.setProperty("java.rmi.server.codebase",
					"http://" + currentHostname + ":" + rmiClassServer.getHttpPort() + "/");
		} catch (SocketException | UnknownHostException e) {
			System.out.println("Unable to get the local address");
			e.printStackTrace();
			System.exit(1);
		}

		try {
			// Ricerca del provider e registrazione
			String completeName = "rmi://" + providerHost + ":" + providerPort + "/" + serviceName;
			Provider p = (Provider) Naming.lookup(completeName);
			p.register("test_room", "Rfid_SL030", new Rfid_SL030());
			System.out.println("Rfid_SL030 registrato");
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			System.out.println("Impossible to register sensor");
			e.printStackTrace();
		}
	}
}
