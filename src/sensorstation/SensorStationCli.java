package sensorstation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.reflections.Reflections;

import http.IpUtils;
import http.RmiClassServer;
import implementations.SensorServer;
import provider.Provider;
import sensor.Sensor;
import sensor.SensorParameter;

public class SensorStationCli {
	private String stationName;
	private String providerHost;
	private int providerPort;
	private BufferedReader console;
	private Provider provider;

	public SensorStationCli(String[] args) {
		
		try {
			loadStationParameters("station.properties");
		} catch (IOException e) {
			System.out.println("Parameters loading from station.properties failed");
			e.printStackTrace();
		}

		console = new BufferedReader(new InputStreamReader(System.in));

		try {
			askForParameters();
		} catch (IOException e1) {
			System.out.println("IOException while asking for parameters");
			e1.printStackTrace();
			System.exit(1);
		}

		try {
			initRmi();
		} catch (SocketException | UnknownHostException e) {
			System.out.println("Unable to get the local address");
			e.printStackTrace();
			System.exit(2);
		}

		while (true)
			mainMenu();
	}

	private void mainMenu() {
		System.out.println("Operazioni disponibili:");
		System.out.println("1.\tAvvia sensore");
		System.out.println("2.\tStato sensore");
		System.out.println("3.\tFerma sensore");
		System.out.print("> ");
		int choice = 0;
		try {
			choice = Integer.parseInt(console.readLine());
		} catch (IOException | NumberFormatException e) {
			e.printStackTrace();
			return;
		}
		switch (choice) {
		case 1:
			List<Class<? extends SensorServer>> list = getServersList();
			if (list.isEmpty()) {
				System.out.println("No loadable sensor classes found");
				break;
			}
			for (int j = 0; j < list.size(); j++) {
				System.out.println(j + ".\t" + list.get(j).getSimpleName());
			}

			System.out.print("> ");
			try {
				choice = Integer.parseInt(console.readLine());
			} catch (IOException | NumberFormatException e) {
				e.printStackTrace();
				return;
			}

			System.out.print("File da cui caricare i parametri? ");
			String propertyFile;
			try {
				propertyFile = console.readLine().trim();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			SensorServer s = initSensor(list.get(choice), new File(propertyFile));
			if (s == null) {
				System.out.println("Errore di inizializzazione");
				return;
			}

			System.out.print("Nome del sensore? ");
			String sensorName = null;
			try {
				sensorName = console.readLine().trim();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			try {
				provider.register(stationName, sensorName, s);
			} catch (RemoteException e) {
				System.out.println("Errore di registrazione");
				e.printStackTrace();
				return;
			}
			break;
		case 2:

			break;
		case 3:

			break;
		default:
			System.out.println("Scelta non riconosciuta: " + choice);
			break;
		}
	}

	private void initRmi() throws SocketException, UnknownHostException {
		// Impostazione del SecurityManager
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}


		String currentHostname = IpUtils.getCurrentIp().getHostAddress();
		System.out.println("currentHostName: " + currentHostname);

		// Avvia un server http affinchè altri possano scaricare gli stub di
		// questa classe
		// da questo codebase in maniera dinamica quando serve
		// (https://publicobject.com/2008/03/java-rmi-without-webserver.html)
		RmiClassServer rmiClassServer = new RmiClassServer();
		rmiClassServer.start();
		System.setProperty("java.rmi.server.hostname", currentHostname);
		System.setProperty("java.rmi.server.codebase", "http://" + currentHostname + ":" + rmiClassServer.getHttpPort() + "/");

		try {
			// Ricerca del providerHost e registrazione
			String completeName = "rmi://" + providerHost + ":" + providerPort + "/" + "ProviderRMI";
			provider = (Provider) Naming.lookup(completeName);
			System.out.println("Connessione al ProviderRMI completata");
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			System.out.println("Impossible to register sensor");
			e.printStackTrace();
		}
	}

	private void askForParameters() throws IOException {
		// TODO: while loop to prevent empty strings
		if (stationName == null) {
			System.out.print("Station name? ");
			stationName = console.readLine().trim();
		}
		if (providerHost == null) {
			System.out.print("Provider address? ");
			providerHost = console.readLine().trim();
		}
		if (providerPort == 0) {
			System.out.print("Provider port? ");
			providerPort = Integer.parseInt(console.readLine().trim());
		}
	}

	public final void loadStationParameters(String propertyFile) throws IOException {
		loadStationParameters(propertyFile, false);
	}

	public void loadStationParameters(String propertyFile, boolean isXml) throws IOException {
		if (propertyFile == null)
			throw new IllegalArgumentException();
		Properties properties = new Properties();
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(propertyFile);
		if (isXml)
			properties.loadFromXML(inputStream);
		else
			properties.load(inputStream);
		inputStream.close();

		stationName = properties.getProperty("stationName", "");
		System.out.println("stationName: " + stationName);
		providerHost = properties.getProperty("providerIp", "");
		System.out.println("providerHost: " + providerHost);
		providerPort = Integer.parseInt(properties.getProperty("providerPort", "0"));
		System.out.println("providerPort: " + providerPort);
	}

	private SensorServer initSensor(Class<? extends SensorServer> sensorClass, File properyFile) {
		assert sensorClass != null;

		SensorServer s;
		try {
			s = sensorClass.getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e1) {
			e1.printStackTrace();
			return null;
		}

		if (properyFile != null) {
			s.loadParametersFromFile(properyFile);
			for (Field f : s.getAllSensorParameterFields())
				try {
					if (f.isAnnotationPresent(SensorParameter.class))
						if (f.get(s) != null)
							System.out.println(f.getName() + ":\t" + f.get(s));
						else {
							// TODO while loop to avoid empty parameters
							System.out.print(f.getName() + "? ");
							String value = console.readLine().trim();
							Class<?> typeToParse = f.getType();
							if (typeToParse == String.class)
								f.set(s, value);
							else if (typeToParse == Integer.class)
								f.set(s, Integer.valueOf(value));
							else if (typeToParse == Double.class)
								f.set(s, Double.valueOf(value));
							else if (typeToParse == Boolean.class)
								f.set(s, Boolean.valueOf(value));
						}
				} catch (IllegalAccessException | IOException e) {
					e.printStackTrace();
					return null;
				}
		}

		try {
			s.setUp();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return s;
	}

	/**
	 * Scans the classes through reflection to find all the subclasses of
	 * {@link SensorServer}
	 * 
	 * @return a list of subclasses of {@link SensorServer}
	 */
	public static List<Class<? extends SensorServer>> getServersList() {
		// https://code.google.com/archive/p/reflections/
		Reflections reflections = new Reflections("implementations");
		Set<Class<? extends SensorServer>> subTypes = reflections.getSubTypesOf(SensorServer.class);
		return new ArrayList<>(subTypes);
	}

	public static void main(String[] args) {
		new SensorStationCli(args);
	}

}
