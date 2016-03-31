
package provider;

import java.io.File;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import http.IpUtils;
import http.RmiClassServer;
import logging.Logs;
import sensor.base.Sensor;
import station.Station;

public class ProviderRMI extends UnicastRemoteObject implements Provider {

	private static String createId(String name, String location) {
		return name + "@" + location;
	}

	private static String[] splitId(String id) {
		return new String[] { id.substring(0, id.indexOf('@')), id.substring(id.indexOf('@')) };
	}

	private static final Logger log = Logger.getLogger(ProviderRMI.class.getName());

	private static final long serialVersionUID = -299631912733255270L;

	// Avvio del Server RMI
	// java provider.ProviderRMI
	public static void main(String[] args) {
		int registryPort = 1099;
		String registryHost = "localhost";
		Logs.createLogFor("PROVIDER");

		if (args.length == 1) {
			try {
				registryPort = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				log.severe("Usage: ProviderRMI [registryPort]");
				System.exit(1);
			}
			if (registryPort < 1024 || registryPort > 65535) {
				log.severe("Port out of range");
				System.exit(2);
			}
		} else if (args.length > 1) {
			log.severe("Too many arguments, usage: ProviderRMI [registryPort]");
			System.exit(-1);
		}

		// Impostazione del SecurityManager
		if (!new File("assets/rmi.policy").canRead()) {
			System.out
					.println("Unable to load security policy, assure that you have rmi.policy in the assets directory");
			System.exit(-3);
		}
		System.setProperty("java.security.policy", "assets/rmi.policy");
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			LocateRegistry.createRegistry(registryPort);
		} catch (RemoteException e) {
			log.log(Level.SEVERE, "Error creating the registry", e);
			System.exit(-1);
		}

		// Avvia un server http affinchè altri possano scaricare gli stub di
		// questa classe
		// da questo codebase in maniera dinamica quando serve
		// (https://publicobject.com/2008/03/java-rmi-without-webserver.html)
		String currentHostname = null;
		try {
			currentHostname = IpUtils.getCurrentIp().getHostAddress();
			RmiClassServer rmiClassServer = new RmiClassServer();
			rmiClassServer.start();
			System.setProperty("java.rmi.server.useCodebaseOnly", "false");
			System.setProperty("java.rmi.server.hostname", currentHostname);
			System.setProperty("java.rmi.server.codebase",
					"http://" + currentHostname + ":" + rmiClassServer.getHttpPort() + "/");
		} catch (SocketException | UnknownHostException e) {
			log.log(Level.SEVERE, "Unable to get the local address", e);
			e.printStackTrace();
			System.exit(1);
		}

		// Registrazione del servizio RMI
		try {
			String completeName = ProviderUtils.buildProviderUrl(registryHost, registryPort);
			ProviderRMI serverRMI = new ProviderRMI();
			Naming.rebind(completeName, serverRMI);
			log.info(ProviderUtils.PROVIDER_NAME + " registered on the rmiregistry");
		} catch (RemoteException | MalformedURLException e) {
			log.log(Level.SEVERE, "Unable to register the provider", e);
			System.exit(3);
		}

		new MulticastProvider(currentHostname, registryPort).start();
	}

	private final Map<String, Sensor> sensorMap;
	private final Map<String, Station> stationMap;

	private ProviderRMI() throws RemoteException {
		super();
		sensorMap = new HashMap<>();
		stationMap = new HashMap<>();
	}

	@Override
	public synchronized Sensor find(String name, String location) throws RemoteException {
		if (location == null || name == null || location.isEmpty() || name.isEmpty())
			throw new RemoteException("Argument error");
		String fullName = createId(name, location);
		Sensor sensor = sensorMap.get(fullName);
		if (sensor == null)
			throw new RemoteException("Sensor " + fullName + " not found");
		log.info("Requested: " + fullName);
		return sensor;
	}

	// TODO Attenzione che ovunque tranne qui i nomi logici dei sensori nella
	// forma nome@location non escono fuori dal provider, cioè nessuno sa che
	// formato usiamo internamente, però questo metodo lo fa uscire all'esterno
	// -> male
	@Override
	public synchronized Map<String, Sensor> findAll(String name, String location, Class<? extends Sensor> type)
			throws RemoteException {
		Map<String, Sensor> result = new HashMap<>();
		sensorMap.forEach((fullname, sensor) -> {
			String[] id = splitId(fullname);
			if ((name == null || id[0].equals(name)) && (location == null || id[1].equals(location))
					&& (type == null || type.isAssignableFrom(sensor.getClass())))
				result.put(fullname, sensor);
		});
		return result;
	}

	@Override
	public synchronized void register(String name, String location, Sensor sensor) throws RemoteException {
		if (location == null || name == null || sensor == null || location.isEmpty() || name.isEmpty())
			throw new RemoteException("Argument error");

		String fullName = createId(name, location);
		if (sensorMap.containsKey(fullName))
			throw new RemoteException("Sensor " + fullName + " already registered");
		sensorMap.put(fullName, sensor);

		String annotation = RMIClassLoader.getClassAnnotation(sensor.getClass());
		log.log(Level.INFO, "Registered: {0}\n\tstub:\t\t{1}\n\tannotation:\t{2}\n\tinterfaces:\t{3}",
				new Object[] { fullName, sensor.getClass().getName(), annotation,
						Stream.of(sensor.getClass().getInterfaces()).filter(Sensor.class::isAssignableFrom)
								.map(Class::getSimpleName).sorted().collect(Collectors.joining(", ")) });
	}

	@Override
	public synchronized void registerStation(String stationName, Station station) throws RemoteException {
		if (stationName == null || stationName.isEmpty())
			throw new RemoteException("Argument error");

		if (stationMap.containsKey(stationName))
			throw new RemoteException("Station " + stationName + " already registered");
		stationMap.put(stationName, station);

		log.info("Registered station: " + stationName);
	}

	@Override
	public synchronized void unregister(String name, String location) throws RemoteException {
		if (location == null || name == null || location.isEmpty() || name.isEmpty())
			throw new RemoteException("Argument error");

		String fullName = createId(name, location);
		sensorMap.remove(fullName);
		log.info("Unregistered: " + fullName);
	}

	@Override
	public synchronized void unregisterStation(String stationName) throws RemoteException {
		if (stationName == null || stationName.isEmpty())
			throw new RemoteException("Argument error");
		if (!stationMap.containsKey(stationName))
			throw new RemoteException("Station " + stationName + " not registered");
		stationMap.remove(stationName);

		log.info("Unregistered station: " + stationName);
	}

	@Override
	public synchronized Station findStation(String location) throws RemoteException {
		if (location == null || location.isEmpty())
			throw new RemoteException("Argument error");
		Station station = stationMap.get(location);
		if (station == null)
			throw new RemoteException("Station " + location + " not found");
		log.info("Requested station: " + location);
		return station;
	}
}
