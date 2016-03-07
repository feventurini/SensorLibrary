package sensorstation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.BindException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.reflections.Reflections;

import http.IpUtils;
import http.RmiClassServer;
import implementations.SensorServer;
import provider.Provider;
import sensor.SensorParameter;

public class SensorStationCli {
	private String stationName;
	private BufferedReader console;
	private Provider provider;
	private String providerUrl;
	private LinkedHashMap<String, SensorServer> activeSensors;

	public SensorStationCli(String[] args) {
		// la riga di comando ha precedenza sulla ricerca in multicast
		providerUrl = null;
		if (args.length == 1)
			try {
				providerUrl = Provider.buildProviderUrl(args[0]);
			} catch (RemoteException ignore) {
				// never thrown
			}
		if (args.length == 2) {
			try {
				providerUrl = Provider.buildProviderUrl(args[0], Integer.parseInt(args[1]));
			} catch (Exception e) {
				System.out.println("Unable to parse the provider address from the command line");
				System.exit(2);
			}
		}

		try {
			loadStationParameters("station.properties");
		} catch (IOException e) {
			System.out.println("Parameters loading from station.properties failed");
			e.printStackTrace();
		}

		if (providerUrl == null) {
			try {
				providerUrl = Provider.findProviderUrl();
			} catch (IOException e) {
				System.out.println("Unable to get the provider address using multicast");
				if (e instanceof BindException)
					System.out.println(
							"Got BindException, maybe the provider is on this machine and has already taken 230.0.0.1");
			}
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
		} catch (SocketException | UnknownHostException | MalformedURLException | RemoteException
				| NotBoundException e) {
			System.out.println("Impossible to find provider");
			e.printStackTrace();
			System.exit(2);
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

		// TODO questo case fa schifo -> cambiare!
		switch (choice) {
		case 1:
			List<Class<? extends SensorServer>> list = getServersList();
			if (list.isEmpty()) {
				System.out.println("No loadable sensor classes found");
				break;
			}
			for (int j = 0; j < list.size(); j++) {
				System.out.println((j + 1) + ".\t" + list.get(j).getSimpleName());
			}

			System.out.print("> ");
			try {
				choice = Integer.parseInt(console.readLine()) - 1;
			} catch (IOException | NumberFormatException e) {
				e.printStackTrace();
				return;
			}

			SensorServer s = initSensor(list.get(choice));
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
				s.tearDown();
				e.printStackTrace();
				return;
			}

			activeSensors.put(sensorName, s);
			break;

		case 2:
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
			break;
		case 3:
			activeSensors.forEach((n, ss) -> System.out.println(n + "\t" + ss.getClass().getSimpleName()));

			System.out.print("> ");
			String key2 = null;
			try {
				key2 = console.readLine();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			if (!activeSensors.containsKey(key2)) {
				System.out.println("Scelta non in lista");
				return;
			}

			try {
				System.out.println(activeSensors.get(key2).getState().toString());
			} catch (RemoteException ignore) {
				// tanto non succede
				System.out.println(ignore.getMessage());
			}

			try {
				SensorServer ss = activeSensors.remove(key2);
				ss.tearDown();
				provider.unregister(stationName, key2);
			} catch (RemoteException ignore) {
				// tanto non succede
				System.out.println(ignore.getMessage());
			}
			break;
		default:
			System.out.println("Scelta non riconosciuta: " + choice);
			break;
		}
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

	private void askForParameters() throws IOException {
		while (stationName == null || stationName.isEmpty()) {
			System.out.print("Station name? ");
			stationName = console.readLine().trim();
		}

		while (providerUrl == null) {
			System.out.print("Provider host? ");
			String providerHost = console.readLine().trim();
			System.out.print("Provider port (enter for 1099)? ");
			int providerPort = 1099;
			try {
				providerPort = Integer.parseInt(console.readLine().trim());
			} catch (NumberFormatException ignore) {
			}
			providerUrl = Provider.buildProviderUrl(providerHost, providerPort);
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

	/**
	 * Performs all the necessary operations to init a sensor: loading its
	 * parameters, asking the user for missing parameters, calling setUp() on
	 * the sensor
	 * 
	 * @param sensorClass
	 * @param properyFile
	 * @return a functioning sensor or null if something went wrong
	 */
	private SensorServer initSensor(Class<? extends SensorServer> sensorClass) {
		assert sensorClass != null;

		SensorServer s;
		try {
			s = sensorClass.getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e1) {
			e1.printStackTrace();
			System.out.println("Impossibile instanziare un sensore");
			return null;
		}

		System.out
				.print("File da cui caricare i parametri (enter for " + sensorClass.getSimpleName() + ".properties)? ");
		try {
			String fileName = console.readLine().trim();
			if (!fileName.trim().isEmpty())
				s.loadParametersFromFile(new File(fileName));
			else
				s.loadParametersFromFile(new File(sensorClass.getSimpleName() + ".properties"));
		} catch (IOException ignore) {
			ignore.printStackTrace();
		}

		for (Field f : s.getAllSensorParameterFields())
			try {
				if (f.isAnnotationPresent(SensorParameter.class))
					if (f.get(s) != null)
						System.out.println(f.getAnnotation(SensorParameter.class).userDescription() + ":\t" + f.get(s));
					else {
						Class<?> typeToParse = f.getType();
						if (!isValidType(typeToParse)) {
							System.out.println("Sensor contains a parameter field that is not parsable from cli input");
							return null;
						}
						String value;
						boolean retry = true;
						do {
							System.out.print(f.getAnnotation(SensorParameter.class).userDescription() + " ("
									+ typeToParse.getSimpleName() + ")? ");
							value = console.readLine().trim();
							if (!value.isEmpty())
								if (typeToParse == String.class) {
									f.set(s, value);
									retry = false;
								} else {
									try {
										Method valueOf = typeToParse.getMethod("valueOf", String.class);
										Object obj = valueOf.invoke(null, value.trim());
										f.set(s, obj);
										retry = false;
									} catch (InvocationTargetException ignore) {
										// probabilemte un problema di parsing
										// dei
										// numeri
										System.out.println("Exception: " + ignore.getTargetException().getMessage());
										retry = true;
									} catch (NoSuchMethodException e) {
										// non dovrebbe mai avvenire perchè
										// tutti i
										// campi di SensorParameter.validTypes
										// hanno il metodo valueOf(String)
										return null;
									}
								}
						} while (retry);
					}
			} catch (IllegalAccessException | IOException e) {
				System.out.println("Eccezione durante la lettura dei parametri: " + e.getMessage());
				e.printStackTrace();
				return null;
			}

		try {
			s.setUp();
		} catch (Exception e) {
			System.out.println("Eccezione durante il setUp del sensore: " + e.getMessage());
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
		Reflections reflections = new Reflections("");
		Set<Class<? extends SensorServer>> subTypes = reflections.getSubTypesOf(SensorServer.class);
		return new ArrayList<>(subTypes);
	}

	private boolean isValidType(Class<?> klass) {
		if (klass == String.class)
			return true;
		else
			try {
				klass.getMethod("valueOf", String.class);
			} catch (NoSuchMethodException e) {
				return false;
			}
		return true;
	}

	public static void main(String[] args) {
		new SensorStationCli(args);
	}

}
