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
public class MulticastProvider extends Thread {
	private static final Logger log = Logger.getLogger(MulticastProvider.class.getName());
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
			log.info("MulticastDiscoveryServer started on " + group.getHostAddress() + ":" + port + ", will send "
					+ currentHostname + ":" + registryPort);
		} catch (IOException e) {
			log.log(Level.WARNING, "MulticastDiscoveryServer not started", e);
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
				log.log(Level.WARNING, "Error during broadcast", e);
			}
		}
	}
}