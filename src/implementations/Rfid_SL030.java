package implementations;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

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

public class Rfid_SL030 extends SensorServer implements RfidSensor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4894977624049261879L;

	// Raspberry Pi's I2C bus
	private static final int i2cBus = 1;

	// RFID addresses
	private static final int DEVICE_ADDRESS = 0x50;

	// Read RFID command
	private static final byte getRfidCmd = (byte) 0x01;
	private static final byte firmwareRfidCmd = (byte) 0xF0;

	// I2C bus
	I2CBus bus;

	@SensorParameter(userDescription = "Trigger pin", propertyName = "trigger")
	public Integer triggerPinNumber;

	private GpioPinDigitalInput trigger;

	private List<FutureResultImpl<String>> queue;

	// Device object
	private static I2CDevice sl030;

	public Rfid_SL030() throws RemoteException {
		queue = new LinkedList<>();

	}

	@Override
	public void setUp() throws Exception {

		if (!allParametersFilledUp()) {
			isSetUp = false;
		}

		else {
			final GpioController gpio = GpioFactory.getInstance();

			System.out.println(triggerPinNumber);
			Pin triggerPin = RaspiPin.getPinByName("GPIO " + triggerPinNumber);
			System.out.println(triggerPin);
			// provision gpio pin #02 as an input pin with its internal pull
			// down
			// resistor enabled
			trigger = gpio.provisionDigitalInputPin(triggerPin, PinPullResistance.PULL_DOWN);
			trigger.setDebounce(1000);

			try {
				bus = I2CFactory.getInstance(I2CBus.BUS_1);
				System.out.println("Connected to bus OK!!!");

				// get device itself
				sl030 = bus.getDevice(DEVICE_ADDRESS);

				System.out.println("Connected to device OK!!!");
				// Small delay before starting
			} catch (IOException e) {
				System.out.println("Exception: " + e.getMessage());
			}

			trigger.addListener(new GpioPinListenerDigital() {
				@Override
				public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
					if (!queue.isEmpty() && event.getState() == PinState.LOW) {
						// something to read
						String rfid = null;
						try {
							rfid = readRfid();
							System.out.println("Letto RFID " + rfid);
							if (!rfid.equals("NO-TAG"))
								synchronized (queue) {
									// scorre tutta la lista prendendo la testa e settando il risultato
									// TODO usare una FIFO
									while(!queue.isEmpty())
										queue.remove(0).set(rfid);									
								}
						} catch (IOException e) {
							e.printStackTrace();
						}

					}
				}

			});

			isSetUp = true;
		}

	}
	

	@Override
	public void tearDown() {
		// TODO Auto-generated method stub

	}

	public String readRfid() throws IOException {

		if (isSetUp == false)
			throw new IllegalStateException("Sensor setup incomplete");

		byte[] writeBuffer = new byte[2];
		String result = "NO-TAG";
		try {
			// SEND COMMAND TO DEVICE TO REQUEST FIRMWARE VERSION
			writeBuffer[0] = (byte) 0x01; // LENGTH (CMD+DATA)
			writeBuffer[1] = (byte) 0x01; // 0x01getRfidCmd; // COMMAND
			sl030.write(writeBuffer, 0, 2);

			Thread.sleep(1000);

			// READ SELECT-TAG RESPONSE
			// first just read in a single byte that represents the
			// command+status+data payload
			int length = sl030.read();

			System.out.println("TOTAL BYTES AVAILABLE: " + length);

			// if there are no remaining bytes (length == 0), then we can exit
			// the function
			if (length <= 0) {
				System.out.format("Error: %n bytes read for LENGTH/n", length);
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
				System.out.format("Error: %n bytes read/n", readTotal);
				return "NO-TAG";
			}

			byte leng = readBuffer[0];
			byte command = readBuffer[1]; // COMMAND BYTE
			byte status = readBuffer[2]; // STATUS BYTES
			byte type = readBuffer[readBuffer.length - 1];
			byte[] uidByte = new byte[readBuffer.length - 4];
			int k = 3;
			for (int i = 0; i < uidByte.length; i++) {
				uidByte[i] = readBuffer[i + k];
			}
			// now we need to get the payload data (if there is any?)
			String uid = "?";

			if (readTotal > 3) {
				// uid=Base64.encodeBase64String(uidByte);
				uid = DatatypeConverter.printHexBinary(uidByte);
			}
			result = uid;

		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
		} catch (InterruptedException e) {
			System.out.println("Interrupted Exception: " + e.getMessage());
		}
		// System.out.println("Id RFID: " + result);
		// if (!result.equalsIgnoreCase("NO-TAG"))
		// {
		// this.Display(result);
		// }
		return result;
	}

	@Override
	public String readTag() throws RemoteException {
		return readTagAsync().get();
	}

	@Override
	public FutureResult<String> readTagAsync() throws RemoteException {
		FutureResultImpl<String> result = new FutureResultImpl<>();
		synchronized (queue) {
			queue.add(result);			
		}
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
