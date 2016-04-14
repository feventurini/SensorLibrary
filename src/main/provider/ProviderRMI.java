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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import http.IpUtils;
import http.RmiClassServer;
import logging.Logs;
import sensor.base.Sensor;
import sensor.base.SensorState;
import station.Station;

public class ProviderRMI extends UnicastRemoteObject implements Provider {
	private static final Logger log = Logger.getLogger(ProviderRMI.class.getName());

	private static final long serialVersionUID = -299631912733255270L;

	// Avvio del Server RMI
	// java provider.ProviderRMI
	public static void main(String[] args) {
		int registryPort = 1098;
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

	private final Map<SensorId, Sensor> sensorMap;
	private final Map<String, Station> stationMap;

	private final ArrayList<RegistrationListener> listeners;

	private ProviderRMI() throws RemoteException {
		super();
		sensorMap = new HashMap<>();
		stationMap = new HashMap<>();
		listeners = new ArrayList<>();
	}

	@Override
	public synchronized Sensor find(SensorId fullName) throws RemoteException {
		if (fullName == null)
			throw new RemoteException("Argument error");
		Sensor sensor = sensorMap.get(fullName);
		if (sensor == null)
			throw new RemoteException("Sensor " + fullName + " not found");
		log.info("Requested: " + fullName);
		return sensor;
	}

	@Override
	public synchronized Map<SensorId, Sensor> findAll(String name, String location, Class<? extends Sensor> type,
			SensorState state) throws RemoteException {
		Map<SensorId, Sensor> result = new HashMap<>();
		sensorMap.forEach((fullname, sensor) -> {
			try {
				if ((name == null || fullname.name.equals(name))
						&& (location == null || fullname.location.equals(location))
						&& (type == null || type.isAssignableFrom(sensor.getClass()))
						&& (state == null || sensor.getState() == state))
					result.put(fullname, sensor);
			} catch (RemoteException e) {
				log.log(Level.WARNING, "Unable to get sensor state", e);
			}
		});
		return result;
	}

	@Override
	public synchronized void register(SensorId fullName, Sensor sensor) throws RemoteException {
		if (fullName == null || sensor == null)
			throw new RemoteException("Argument error");

		if (sensorMap.containsKey(fullName)) {
			log.info("Requested to register sensor " + fullName + " that is already registered");
			throw new RemoteException("Sensor " + fullName + " already registered");
		}
		sensorMap.put(fullName, sensor);

		String annotation = RMIClassLoader.getClassAnnotation(sensor.getClass());
		log.log(Level.INFO, "Registered: {0}\n\tstub:\t\t{1}\n\tannotation:\t{2}\n\tinterfaces:\t{3}",
				new Object[] { fullName, sensor.getClass().getName(), annotation, sensor.getSensorInterfaces().stream()
						.map(Class::getSimpleName).sorted().collect(Collectors.joining(", ")) });

		for (RegistrationListener l : listeners)
			try {
				l.onSensorRegistered(fullName, sensor);
			} catch (RemoteException e) {
				log.log(Level.WARNING, "Exception in RegistrationListener", e.getCause());
			}
	}

	@Override
	public synchronized void registerStation(String stationName, Station station) throws RemoteException {
		if (stationName == null || stationName.isEmpty())
			throw new RemoteException("Argument error");

		if (stationMap.containsKey(stationName)) {
			log.info("Requested to register station " + stationName + " that is already registered");
			throw new RemoteException("Station " + stationName + " already registered");
		}
		stationMap.put(stationName, station);

		log.info("Registered station: " + stationName);
		for (RegistrationListener l : listeners)
			try {
				l.onStationRegistered(stationName, station);
			} catch (RemoteException e) {
				log.log(Level.WARNING, "Exception in RegistrationListener", e.getCause());
			}
	}

	@Override
	public synchronized void unregister(SensorId fullName) throws RemoteException {
		if (fullName == null)
			throw new RemoteException("Argument error");

		Sensor sensor = sensorMap.remove(fullName);
		if (sensor == null) {
			log.info("Requested to unregister sensor " + fullName + " that is not registered");
		} else {
			log.info("Unregistered: " + fullName);
			for (RegistrationListener l : listeners)
				try {
					l.onSensorUnRegistered(fullName, sensor);
				} catch (RemoteException e) {
					log.log(Level.WARNING, "Exception in RegistrationListener", e.getCause());
				}
		}
	}

	@Override
	public synchronized void unregisterStation(String stationName) throws RemoteException {
		if (stationName == null || stationName.isEmpty())
			throw new RemoteException("Argument error");
		if (!stationMap.containsKey(stationName))
			throw new RemoteException("Station " + stationName + " not registered");
		Station station = stationMap.remove(stationName);

		if (station == null) {
			log.info("Requested to unregister station " + stationName + " that is not registered");
		} else {
			log.info("Unregistered station: " + stationName);
			for (RegistrationListener l : listeners)
				try {
					l.onStationUnRegistered(stationName, station);
				} catch (RemoteException e) {
					log.log(Level.WARNING, "Exception in RegistrationListener", e.getCause());
				}
		}
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

	@Override
	public void addRegistrationListener(RegistrationListener listener) throws RemoteException {
		listeners.add(listener);
		log.info("Registered listener");
	}

	@Override
	public void removeRegistrationListener(RegistrationListener listener) throws RemoteException {
		listeners.remove(listener);
		log.info("Removed listener");
	}
}
