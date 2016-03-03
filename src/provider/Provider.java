package provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.Remote;
import java.rmi.RemoteException;

import http.IpUtils;
import sensor.Sensor;

public interface Provider extends Remote {
	public static final String PROVIDER_NAME = "ProviderRMI";
	public static final int PROVIDER_PORT = 1099;

	/**
	 * If possible finds the sensor registered with the name and location
	 * provided
	 * 
	 * @param location
	 *            the location
	 * @param name
	 *            the name
	 * @return a remote reference to the sensor
	 * @throws RemoteException
	 *             if the sensor were not found
	 */
	public Sensor find(String location, String name) throws RemoteException;

	/**
	 * If possible finds the all the sensors registered with the name and
	 * location provided
	 * 
	 * @param location
	 *            the location (null for any location)
	 * @param name
	 *            the name (null for any name)
	 * @return an array of remote references to the sensors, possibly with 0
	 *         length
	 * @throws RemoteException
	 */
	public Sensor[] findAll(String location, String name) throws RemoteException;

	/**
	 * Register a new Sensor
	 * 
	 * @param location
	 *            the location
	 * @param name
	 *            the name
	 * @param sensor
	 *            the sensor itself
	 * @throws RemoteException
	 *             if the registration was not possible
	 */
	public void register(String location, String name, Sensor sensor) throws RemoteException;

	/**
	 * Unregister a sensor
	 * 
	 * @param location
	 *            the location
	 * @param name
	 *            the name
	 * @throws RemoteException
	 *             if the unregistration was not possible
	 */
	public void unregister(String location, String name) throws RemoteException;
	
	public static String buildProviderUrl(String host) {
		return buildProviderUrl(host, 1099);
	}
	
	public static String buildProviderUrl(String host, int port) {
		return "rmi://" + host + ":" + port + "/" + PROVIDER_NAME;
	}

	public static String findProviderUrl() throws IOException {
		// multicast socket to send requests (e non datagramsocket su
		// 192.168.0.255)
		InetAddress group = InetAddress.getByName("230.0.0.1");
		int port = 5000;
		MulticastSocket ms = new MulticastSocket(port);
		ms.joinGroup(group);

		// datagram socket to receive a response
		InetAddress localaddress = IpUtils.getCurrentIp();
		DatagramSocket ds = new DatagramSocket(port, localaddress);
		ds.setSoTimeout(5000);

		// request containing the local address
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeUTF(localaddress.getHostName());
		dos.writeInt(ds.getLocalPort());
		byte data[] = baos.toByteArray();
		dos.close();
		baos.close();
		DatagramPacket request = new DatagramPacket(data, data.length, group, port);

		// packet for the response
		data = new byte[20];
		DatagramPacket packet = new DatagramPacket(data, data.length);

		int attempts = 0;
		InetAddress providerHost = null;
		int providerPort = 0;
		do {
			// sending request
			ms.send(request);
			System.out.println("Search for provider started on " + group.getHostAddress() + ":" + port);

			// receiving response
			packet.setData(data);
			ds.receive(packet);
			ByteArrayInputStream bias = new ByteArrayInputStream(packet.getData());
			DataInputStream dis = new DataInputStream(bias);
			try {
				providerHost = InetAddress.getByName(dis.readUTF());
				providerPort = dis.readInt();
			} catch (IOException ignore) {
			}
			dis.close();
			bias.close();
		} while (++attempts <= 5 && (providerHost == null || providerHost.isLoopbackAddress()));
		ms.leaveGroup(group);
		ms.close();
		ds.close();
		return buildProviderUrl(providerHost.getHostAddress(), providerPort);
	}
	
	
}
