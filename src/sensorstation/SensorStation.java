package sensorstation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.reflections.Reflections;

import http.IpUtils;
import http.RmiClassServer;
import implementations.SensorServer;
import provider.Provider;

public class SensorStation {
	/**
	 * Scans the classes through reflection to find all the subclasses of
	 * {@link SensorServer}
	 *
	 * @return a list of subclasses of {@link SensorServer}
	 */
	public static List<Class<? extends SensorServer>> getServersList() {
		// https://code.google.com/archive/p/reflections/
		Reflections reflections = new Reflections("");
		Set<Class<? extends SensorServer>> subTypes = reflections.getSubTypesOf(SensorServer.class);
		return new ArrayList<>(subTypes);
	}

	public static void main(String[] args) {
		new SensorStation(args);
	}

	private String stationName;
	private BufferedReader console;
	private Provider provider;

	private String providerUrl;

	private LinkedHashMap<String, SensorServer> activeSensors;

	public SensorStation(String[] args) {
		if (args == null || args.length != 1) {
			System.out.println("Usage: SensorStation propertyFile");
			System.exit(-1);
		}

		providerUrl = null;
		try {
			loadStationParameters(args[0]);
		} catch (IOException e) {
			System.out.println("Parameters loading from " + args[0] + " failed");
			e.printStackTrace();
			System.exit(-2);
		}

		if (providerUrl == null) {
			try {
				providerUrl = Provider.findProviderUrl();
			} catch (IOException e) {
				System.out.println("Unable to get the provider address using multicast");
			}
		}

		try {
			initRmi();
		} catch (SocketException | UnknownHostException | MalformedURLException | RemoteException
				| NotBoundException e) {
			System.out.println("Impossible to find provider");
			e.printStackTrace();
			System.exit(-3);
		}

		activeSensors = new LinkedHashMap<>();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			activeSensors.forEach((n, s) -> {
				s.tearDown();
				try {
					provider.unregister(stationName, n);
				} catch (Exception e) {
					System.out.println("Error unregistering " + n);
					e.printStackTrace();
				}
			});
		}));
	}

	private void initRmi()
			throws SocketException, UnknownHostException, MalformedURLException, RemoteException, NotBoundException {
		// Impostazione del SecurityManager
		if (!new File("rmi.policy").canRead()) {
			System.out.println(
					"Unable to load security policy, assure that you have rmi.policy in the directory you launched ProviderRMI in");
			System.exit(-3);
		}
		System.setProperty("java.security.policy", "rmi.policy");
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
		System.setProperty("java.rmi.server.codebase",
				"http://" + currentHostname + ":" + rmiClassServer.getHttpPort() + "/");

		// Ricerca del providerHost e registrazione
		System.out.println("Looking up provider on: " + providerUrl);
		provider = (Provider) Naming.lookup(providerUrl);
		System.out.println("Connessione al ProviderRMI completata");
	}

	/**
	 * Performs all the necessary operations to init a sensor: loading its
	 * parameters, asking the user for missing parameters, calling setUp() on
	 * the sensor
	 *
	 * @param sensorClass
	 * @param properyFile
	 * @return a functioning sensor or null if something went wrong
	 */
	private void initSensor(Class<? extends SensorServer> sensorClass, Properties sensorParams) {
		assert sensorClass != null && sensorParams != null;
		if (!sensorParams.containsKey("name"))
			throw new IllegalArgumentException("Sensor name not found");

		SensorServer s;
		try {
			s = sensorClass.getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e1) {
			e1.printStackTrace();
			System.out.println("Impossibile creare un'istanza di " + sensorClass.getName());
			return;
		}
		s.loadParameters(sensorParams);

		try {
			s.setUp();
		} catch (Exception e) {
			System.out.println("Eccezione durante il setUp del sensore: " + e.getMessage());
			e.printStackTrace();
			return;
		}

		String sensorName = sensorParams.getProperty("name");
		try {
			provider.register(stationName, sensorName, s);
		} catch (RemoteException e) {
			System.out.println("Errore di registrazione: " + e.getMessage());
			s.tearDown();
			return;
		}

		activeSensors.put(sensorName, s);
	}

	public final void loadStationParameters(String propertyFile) throws IOException {
		loadStationParameters(propertyFile, false);
	}

	public void loadStationParameters(String propertyFile, boolean isXml) throws IOException {
		if (propertyFile == null)
			throw new IllegalArgumentException();
		Properties properties = new Properties();
		InputStream inputStream = new FileInputStream(propertyFile);
		if (isXml) {
			properties.loadFromXML(inputStream);
		} else {
			properties.load(inputStream);
		}
		inputStream.close();

		if (!properties.containsKey("stationName"))
			throw new IOException("stationName not found in " + propertyFile);
		if (!properties.containsKey("sensorsToLoad"))
			throw new IOException("sensorsToLoad not found in " + propertyFile);

		stationName = properties.getProperty("stationName");
		System.out.println("stationName: " + stationName);

		String sensorsToLoad = properties.getProperty("sensorsToLoad");
		for (String sensor : sensorsToLoad.split(";")) {
			String klass = sensor.substring(0, sensor.indexOf(',')).trim();
			String paramFile = sensor.substring(sensor.indexOf(',') + 1).trim();
			Properties p = new Properties();
			p.load(new FileInputStream(paramFile));
			try {
				initSensor((Class<? extends SensorServer>) getClass().getClassLoader().loadClass(klass), p);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// checks if the file specifics a provider host+port
		if (providerUrl == null && properties.containsKey("providerIp")) {
			String providerHost = properties.getProperty("providerIp", "");
			System.out.println("providerHost: " + providerHost);
			// providerPort property is optional
			int providerPort = Integer.parseInt(properties.getProperty("providerPort", "1099"));
			System.out.println("providerPort: " + providerPort);
			providerUrl = Provider.buildProviderUrl(providerHost, providerPort);
		}

	}

//	private void startNewSensor() {
//		List<Class<? extends SensorServer>> list = getServersList();
//		if (list.isEmpty()) {
//			System.out.println("No loadable sensor classes found");
//			return;
//		}
//		for (int j = 0; j < list.size(); j++) {
//			System.out.println(j + 1 + ".\t" + list.get(j).getSimpleName());
//		}
//
//		System.out.print("> ");
//		int choice = -1;
//		try {
//			choice = Integer.parseInt(console.readLine()) - 1;
//		} catch (IOException | NumberFormatException e) {
//			e.printStackTrace();
//			return;
//		}
//
//		SensorServer s = initSensor(list.get(choice));
//		if (s == null) {
//			System.out.println("Errore di inizializzazione");
//			return;
//		}
//
//		System.out.print("Nome del sensore? ");
//		String sensorName = null;
//		try {
//			sensorName = console.readLine().trim();
//		} catch (IOException e) {
//			e.printStackTrace();
//			return;
//		}
//
//		try {
//			provider.register(stationName, sensorName, s);
//		} catch (RemoteException e) {
//			System.out.println("Errore di registrazione");
//			s.tearDown();
//			e.printStackTrace();
//			return;
//		}
//
//		activeSensors.put(sensorName, s);
//	}

	private void stopSensor() {
		activeSensors.forEach((n, ss) -> System.out.println(n + "\t" + ss.getClass().getSimpleName()));
		System.out.print("> ");
		String key = null;
		try {
			key = console.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		if (!activeSensors.containsKey(key)) {
			System.out.println("Scelta non in lista");
			return;
		}

		try {
			System.out.println(activeSensors.get(key).getState().toString());
		} catch (RemoteException ignore) {
			// tanto non succede
			System.out.println(ignore.getMessage());
		}

		try {
			SensorServer ss = activeSensors.remove(key);
			ss.tearDown();
			provider.unregister(stationName, key);
		} catch (RemoteException ignore) {
			// tanto non succede
			System.out.println(ignore.getMessage());
		}
	}

	private void watchSensorStatus() {
		if (activeSensors.isEmpty()) {
			System.out.println("No active sensors");
			return;
		}

		activeSensors.forEach((n, ss) -> System.out.println(n + "\t" + ss.getClass().getSimpleName()));

		System.out.print("> ");
		String key = null;
		try {
			key = console.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		if (!activeSensors.containsKey(key)) {
			System.out.println("Scelta non in lista");
			return;
		}

		try {
			System.out.println(activeSensors.get(key).getState().toString());
		} catch (RemoteException ignore) {
			// tanto non succede
			System.out.println(ignore.getMessage());
		}
	}

}
