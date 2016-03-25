
package provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.UnicastRemoteObject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import http.IpUtils;
import http.RmiClassServer;
import sensor.base.Sensor;
import sensor.mocks.Temp4000;
import station.Station;

public class ProviderRMI extends UnicastRemoteObject implements Provider {
	private static final Logger log = Logger.getLogger(ProviderRMI.class.getName());

	/**
	 * Thread che attiva un servizio di discovery del provider in multicast.
	 *
	 * Sfrutta due porte Datagram, una multicast (indirizzo 230.0.0.1 porta
	 * 5000) e una datagram, la prima per ricevere le richieste di discovery dai
	 * clienti e la seconda per inviare le risposte ai singoli.<br>
	 * Le richieste devono rappresentare l'indirizzo ip e la porta a cui inviare
	 * la risposta nel formato stringa + intero. Le risposte contengono
	 * indirizzo ip + porta del provider nello stesso formato.
	 */
	public static class MulticastProvider extends Thread {
		private String currentHostname;
		private int registryPort;

		public MulticastProvider(String currentHostname, int registryPort) {
			this.currentHostname = currentHostname;
			this.registryPort = registryPort;
		}

		@Override
		public void run() {
			InetAddress group;
			int port;
			MulticastSocket ms;
			DatagramSocket ds;
			DatagramPacket response;
			DatagramPacket request;
			byte[] responsePayload = null;

			// setup
			try {
				// multicast socket to receive requests
				group = InetAddress.getByName("230.0.0.1");
				port = 5000;
				ms = new MulticastSocket(port);
				ms.joinGroup(group);
				// datagram socket to send responses
				ds = new DatagramSocket();
				// preparing the response with provider ip + provide port
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				dos.writeUTF(currentHostname);
				dos.writeInt(registryPort);
				responsePayload = baos.toByteArray();
				dos.close();
				baos.close();
				System.out.println("MulticastDiscoveryServer started on " + group.getHostAddress() + ":" + port
						+ ", will send " + currentHostname + ":" + registryPort);
			} catch (IOException e) {
				System.out.println("MulticastDiscoveryServer not started");
				e.printStackTrace();
				return;
			}

			while (true) {
				try {
					request = new DatagramPacket(new byte[20], 20);
					ms.receive(request);
					// reading requestor address
					ByteArrayInputStream bias = new ByteArrayInputStream(request.getData());
					DataInputStream dis = new DataInputStream(bias);
					InetAddress requestor = InetAddress.getByName(dis.readUTF());
					int requestorPort = dis.readInt();
					dis.close();
					bias.close();
					// sending response
					response = new DatagramPacket(responsePayload, responsePayload.length, requestor, requestorPort);
					ds.send(response);
				} catch (IOException e) {
					System.out.println("Error during broadcast");
					e.printStackTrace();
				}
			}
		}
	}

	private static final long serialVersionUID = -299631912733255270L;

	// Avvio del Server RMI
	// java provider.ProviderRMI
	public static void main(String[] args) {
		int registryPort = 1099;
		String registryHost = "localhost";

		try {
			Logger global = Logger.getLogger("");
			FileHandler txt = new FileHandler(
					"Provider_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".log");
			txt.setFormatter(new SimpleFormatter());
			FileHandler html = new FileHandler(
					"Provider_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".html");
			html.setFormatter(new Formatter() {
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

				// this method is called for every log records
				public String format(LogRecord rec) {
					StringBuffer buf = new StringBuffer(100);
					buf.append("<tr>");
					// colorize WARNING in yellow, SEVERE in red
					if (rec.getLevel().intValue() == Level.SEVERE.intValue()) {
						buf.append("<td style=\"color:red; font-weight:bold;\">");
					} else if (rec.getLevel().intValue() == Level.WARNING.intValue()) {
						buf.append("<td style=\"color:yellow; font-weight:bold;\">");
					} else {
						buf.append("<td>");
					}
					buf.append(rec.getLevel());
					buf.append("</td><td>");
					buf.append(dateFormat.format(new Date(rec.getMillis())));
					buf.append("</td><td>");
					buf.append(formatMessage(rec));
					buf.append("</td></tr>");

					return buf.toString();
				}

				// this method is called just after the handler using this
				// formatter is created
				public String getHead(Handler h) {
					return "<!DOCTYPE html><head><style>\n" + "table { width: 100% }\n"
							+ "th { font:bold 10pt Tahoma; }\n" + "td { font:normal 10pt Tahoma; }\n"
							+ "h1 {font:normal 11pt Tahoma;}\n" + "</style>\n" + "</head>\n" + "<body>\n" + "<h1>"
							+ dateFormat.format(new Date()) + "</h1>\n"
							+ "<table border=\"0\" cellpadding=\"5\" cellspacing=\"3\">\n" + "<tr align=\"left\">\n"
							+ "\t<th style=\"width:10%\">Loglevel</th>\n" + "\t<th style=\"width:15%\">Time</th>\n"
							+ "\t<th style=\"width:75%\">Log Message</th>\n" + "</tr>\n";
				}

				// this method is called just after the handler using this
				// formatter is closed
				public String getTail(Handler h) {
					return "</table></body></html>";
				}
			});
			global.addHandler(txt);
			global.addHandler(html);
			global.setLevel(Level.CONFIG);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}

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
		if (!new File("rmi.policy").canRead()) {
			log.severe(
					"Unable to load security policy, assure that you have rmi.policy in the directory you launched ProviderRMI in");
			System.exit(-3);
		}
		System.setProperty("java.security.policy", "rmi.policy");
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			LocateRegistry.createRegistry(registryPort);
		} catch (RemoteException e) {
			e.printStackTrace();
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
			log.severe("Unable to get the local address");
			e.printStackTrace();
			System.exit(1);
		}

		// Registrazione del servizio RMI
		try {
			String completeName = ProviderUtils.buildProviderUrl(registryHost, registryPort);
			ProviderRMI serverRMI = new ProviderRMI();
			Naming.rebind(completeName, serverRMI);
			log.info(PROVIDER_NAME + " registered on the rmiregistry");
		} catch (RemoteException | MalformedURLException e) {
			e.printStackTrace();
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
	public synchronized Sensor find(String location, String name) throws RemoteException {
		if (location == null || name == null || location.isEmpty() || name.isEmpty())
			throw new RemoteException("Argument error");
		String fullName = name + "@" + location;
		Sensor sensor = sensorMap.get(fullName);
		if (sensor == null)
			throw new RemoteException("Sensor " + fullName + " not found");
		log.info("Requested: " + fullName);
		return sensor;
	}

	/**
	 * @param location
	 *            or "" for any location
	 * @param name
	 *            or "" for any location
	 * @return
	 * @throws RemoteException
	 */
	@Override
	public synchronized List<Sensor> findAll(String location, String name) throws RemoteException {
		if (location == null || name == null)
			throw new RemoteException("Argument error");

		List<Sensor> result = new LinkedList<Sensor>();
		if (!location.isEmpty() && !name.isEmpty()) {
			String fullName = name + "@" + location;
			sensorMap.forEach((n, s) -> {
				if (n.equals(fullName)) {
					result.add(s);
				}
			});
		} else if (!location.isEmpty()) {
			sensorMap.forEach((n, s) -> {
				if (n.substring(n.indexOf('@')).equals(location)) {
					result.add(s);
				}
			});
		} else if (!name.isEmpty()) {
			sensorMap.forEach((n, s) -> {
				if (n.substring(n.indexOf('@')).equals(location)) {
					result.add(s);
				}
			});
		} else {
			result.addAll(sensorMap.values());
		}
		return result;
	}

	@Override
	public synchronized void register(String location, String name, Sensor sensor) throws RemoteException {
		if (location == null || name == null || sensor == null || location.isEmpty() || name.isEmpty()
				|| name.indexOf('@') != -1)
			throw new RemoteException("Argument error");

		String fullName = name + "@" + location;
		if (sensorMap.containsKey(fullName))
			throw new RemoteException("Sensor " + fullName + " already registered");
		sensorMap.put(fullName, sensor);

		String annotation = RMIClassLoader.getClassAnnotation(sensor.getClass());
		log.log(Level.INFO, "Registered: {0}\n\tstub:\t\t{1}\n\tannotation:\t{2}\n\tinterfaces:\t{3}",
				new Object[] {fullName, sensor.getClass().getName(), annotation,
				Stream.of(sensor.getClass().getInterfaces()).filter(Sensor.class::isAssignableFrom)
						.map(Class::getSimpleName).sorted().collect(Collectors.joining(", "))});
	}

	@Override
	public synchronized void unregister(String location, String name) throws RemoteException {
		if (location == null || name == null || location.isEmpty() || name.isEmpty() || name.indexOf('@') != -1)
			throw new RemoteException("Argument error");

		String fullName = name + "@" + location;
		sensorMap.remove(fullName);
		log.info("Unregistered: " + fullName);
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
	public synchronized void unregisterStation(String stationName) throws RemoteException {
		if (stationName == null || stationName.isEmpty())
			throw new RemoteException("Argument error");
		if (!stationMap.containsKey(stationName))
			throw new RemoteException("Station " + stationName + " not registered");
		stationMap.remove(stationName);

		log.info("Unregistered station: " + stationName);
	}
}
