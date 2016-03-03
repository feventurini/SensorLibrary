package provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
import java.rmi.server.RMIClassLoader;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import http.IpUtils;
import http.RmiClassServer;
import sensor.Sensor;

public class ProviderRMI extends UnicastRemoteObject implements Provider {
	private static final long serialVersionUID = 1L;

	private final Map<String, Sensor> bindings;

	private ProviderRMI() throws RemoteException {
		super();
		bindings = new HashMap<>();
	}

	@Override
	public synchronized Sensor find(String location, String name) throws RemoteException {
		if (location == null || name == null || location.isEmpty() || name.isEmpty())
			throw new RemoteException("Argument error");
		String fullName = name + "@" + location;
		Sensor sensor = bindings.get(fullName);
		if (sensor == null)
			throw new RemoteException("Sensor " + fullName + " not found");
		String annotation = RMIClassLoader.getClassAnnotation(sensor.getClass());
		System.out.println(
				"Requested: " + fullName + " (" + sensor.getClass().getName() + " loaded from " + annotation + ")");
		return sensor;
	}

	@Override
	public synchronized Sensor[] findAll(String location, String name) throws RemoteException {
		if (location == null || name == null || location.isEmpty() || name.isEmpty())
			throw new RemoteException("Argument error");

		return null;
	}

	@Override
	public synchronized void register(String location, String name, Sensor sensor) throws RemoteException {
		if (location == null || name == null || sensor == null || location.isEmpty() || name.isEmpty())
			throw new RemoteException("Argument error");

		String fullName = name + "@" + location;
		if (bindings.containsKey(fullName))
			throw new RemoteException("Sensor " + fullName + " already registered");
		bindings.put(fullName, sensor);

		String annotation = RMIClassLoader.getClassAnnotation(sensor.getClass());
		System.out.println(
				"Registered: " + fullName + " (" + sensor.getClass().getName() + " loaded from " + annotation + ")");
	}

	@Override
	public synchronized void unregister(String location, String name) throws RemoteException {
		if (location == null || name == null || location.isEmpty() || name.isEmpty())
			throw new RemoteException("Argument error");

		String fullName = name + "@" + location;
		bindings.remove(fullName);
		System.out.println("Unregistered: " + fullName);
	}

	// Avvio del Server RMI
	// java -Djava.security.policy=rmi.policy provider.ProviderRMI
	public static void main(String[] args) {
		int registryPort = 1099;
		String registryHost = "localhost";
		String serviceName = "ProviderRMI";

		if (args.length == 1) {
			try {
				registryPort = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.out.println("Usage: ProviderRMI [registryPort]");
				System.exit(1);
			}
			if (registryPort < 1024 || registryPort > 65535) {
				System.out.println("Port out of range");
				System.exit(2);
			}
		} else if (args.length > 1) {
			System.out.println("Too many arguments");
			System.out.println("Usage: ProviderRMI [registryPort]");
			System.exit(-1);
		}

		// Impostazione del SecurityManager
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		// TODO un giorno proviamo se rmiregistry si può far partire da qua
		// try {
		// LocateRegistry.createRegistry(registryPort);
		// } catch (RemoteException e) {
		// e.printStackTrace();
		// System.exit(-1);
		// }

		// Avvia un server http affinchè altri possano scaricare gli stub di
		// questa classe
		// da questo codebase in maniera dinamica quando serve
		// (https://publicobject.com/2008/03/java-rmi-without-webserver.html)
		String currentHostname = null;
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

		// Registrazione del servizio RMI
		String completeName = "//" + registryHost + ":" + registryPort + "/" + serviceName;
		try {
			ProviderRMI serverRMI = new ProviderRMI();
			Naming.rebind(completeName, serverRMI);
			System.out.println("Servizio " + serviceName + " registrato");
		} catch (RemoteException | MalformedURLException e) {
			e.printStackTrace();
			System.exit(3);
		}

		new MulticastProvider(currentHostname, registryPort).start();
	}

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
			byte[] providerIp = null;
			// setup
			try {
				// multicast socket to receive requests
				group = InetAddress.getByName("230.0.0.1");
				port = 5000;
				ms = new MulticastSocket(port);
				ms.joinGroup(group);
				// datagram socket to send responses
				ds = new DatagramSocket();
				// response to send with provider ip
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				dos.writeUTF(currentHostname);
				dos.writeInt(registryPort);
				providerIp = baos.toByteArray();
				dos.close();
				baos.close();
				System.out.println("MulticastDiscoveryServer started on " + group.getHostAddress() + ":" + port);
			} catch (IOException e) {
				System.out.println("MulticastDiscoveryServer not started");
				e.printStackTrace();
				return;
			}

			while (true)
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
					response = new DatagramPacket(providerIp, providerIp.length, requestor, requestorPort);
					ds.send(response);

				} catch (IOException e) {
					System.out.println("Error during broadcast");
					e.printStackTrace();
				}
		}
	}
}
