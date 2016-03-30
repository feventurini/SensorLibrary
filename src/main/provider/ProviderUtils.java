/**
 *
 */
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
import java.util.logging.Level;
import java.util.logging.Logger;

import http.IpUtils;

/**
 * Contains methods to build the provider rmi url from its hostname (and port),
 * to find the provider over the network using datagram packets sent to a
 * multicast group.
 */
public class ProviderUtils {
	private static final Logger log = Logger.getLogger(ProviderUtils.class.getName());

	/**
	 * Builds the provider url in the form "rmi://host:port/name"
	 *
	 * @param host
	 * @return the providder url
	 */
	public static String buildProviderUrl(String host) {
		return buildProviderUrl(host, 1099);
	}

	/**
	 * Builds the provider url in the form "rmi://host:port/name"
	 *
	 * @param host
	 * @param port
	 * @return the provider url
	 */
	public static String buildProviderUrl(String host, int port) {
		return "rmi://" + host + ":" + port + "/" + ProviderUtils.PROVIDER_NAME;
	}

	/**
	 * By means of a multicast request on 230.0.0.1:5000 attempts to locate the
	 * provider, returning its url. The caller must have the permissions to know
	 * its own ip address, to open a multicast socket and a datagram socket
	 *
	 * @return the provider url in the form "rmi://host:port/name"
	 * @throws IOException
	 */
	public static String findProviderUrl() throws IOException {
		// multicast socket to send requests (e non datagramsocket su
		// 192.168.0.255)
		InetAddress group = InetAddress.getByName("230.0.0.1");
		int port = 5000;
		MulticastSocket ms = new MulticastSocket(port);
		ms.joinGroup(group);

		// datagram socket to receive a response
		InetAddress localaddress = IpUtils.getCurrentIp();
		DatagramSocket ds = new DatagramSocket();
		ds.setSoTimeout(30000);

		// request containing the local address
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeUTF(localaddress.getHostName());
		dos.writeInt(ds.getLocalPort());
		byte requestPayload[] = baos.toByteArray();
		dos.close();
		baos.close();
		DatagramPacket request = new DatagramPacket(requestPayload, requestPayload.length, group, port);

		// packet for the response
		byte[] responsePayload = new byte[20];
		DatagramPacket response = new DatagramPacket(responsePayload, responsePayload.length);

		int attempts = 0;
		InetAddress providerHost = null;
		int providerPort = 0;
		do {
			// sending request
			ms.send(request);
			log.info("Search for provider started on " + group.getHostAddress() + ":" + port);

			// receiving response
			ds.receive(response);
			ByteArrayInputStream bias = new ByteArrayInputStream(response.getData());
			DataInputStream dis = new DataInputStream(bias);
			try {
				providerHost = InetAddress.getByName(dis.readUTF());
				providerPort = dis.readInt();
			} catch (IOException e) {
				log.log(Level.WARNING, "Error reading from datagram packet", e);
			}
			dis.close();
			bias.close();
		} while (++attempts <= 5 && (providerHost == null || providerHost.isLoopbackAddress()));
		ms.leaveGroup(group);
		ms.close();
		ds.close();
		return buildProviderUrl(providerHost.getHostAddress(), providerPort);
	}

	public static final String PROVIDER_NAME = "ProviderRMI";
	public static final int PROVIDER_PORT = 1099;

}
