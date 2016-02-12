package discovery;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

public class DiscoveryServer {

	private static final int PORT = 2000; // porta da usare se non specificata

	// NON USO InetAddress ma una stringa per dover fare la conversione da Inet
	// a stringa ogni invio ma solo alla registrazione di un luogo
	public static void main(String[] args) {

		System.out.println("Server: avviato");

		DatagramSocket socket = null;
		DatagramPacket packet = null;
		byte[] buf = new byte[256];
		int port = -1;

		// controllo argomenti (possibile specificare porta)
		if ((args.length == 0)) {
			port = PORT;
		} else if (args.length == 1) {
			try {
				port = Integer.parseInt(args[0]);
				// controllo porta nel caso ci sia
				if (port < 1024 || port > 65535) {
					System.out.println("Porta non corretta, (1024-65535");
					System.exit(1);
				}
			} catch (NumberFormatException e) {
				System.out.println("Usage: java Server [serverPort]");
				System.exit(1);
			}
		} else {
			System.out.println("Usage: java Server [serverPort]");
			System.exit(1);
		}

		try {

			// creo socket e preparo DatagramPacket
			socket = new DatagramSocket(port);
			packet = new DatagramPacket(buf, buf.length);
			System.out.println("Creata la socket: " + socket);
		} catch (SocketException e) {
			System.out.println("Problemi nella creazione della socket: ");
			e.printStackTrace();
			System.exit(1);
		}

		try {

			ByteArrayInputStream biStream = null;
			DataInputStream diStream = null;
			ByteArrayOutputStream boStream = null;
			DataOutputStream doStream = null;
			byte[] data = null;

			Map<String, String> locations = new HashMap<>(); // locationIP,
																// location
			Map<String, Integer> sensors = new HashMap<>(); // location:sensorName,
															// sensorPort

			while (true) {
				System.out.println("\nIn attesa di richieste...");

				// ricezione del datagramma
				try {
					packet.setData(buf);
					socket.receive(packet);
				} catch (IOException e) {
					System.err.println("Problemi nella ricezione del datagramma: " + e.getMessage());
					e.printStackTrace();
					continue;
				}

				boolean result = false;
				try {
					biStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
					diStream = new DataInputStream(biStream);

					switch (diStream.readChar()) {
					case 's':
						int sensorPort = diStream.readInt();
						String sensorName = diStream.readUTF();
						String location = locations.get(packet.getAddress().getHostAddress());
						result = location != null;
						if (result)
							sensors.put(location + ":" + sensorName, sensorPort);
						break;
					case 'l':
						String locationName = diStream.readUTF();
						String locationAddress = packet.getAddress().getHostAddress();
						result = !locations.containsKey(locationAddress);
						if (result)
							locations.put(locationAddress, locationName);
						break;
					}

				} catch (Exception e) {
					System.err.println("Problemi nella lettura della richiesta");
					e.printStackTrace();
					continue;

				}

				// invio datagram di risposta
				try {
					boStream = new ByteArrayOutputStream();
					doStream = new DataOutputStream(boStream);

					doStream.writeChar(result ? 'y' : 'n');

					data = boStream.toByteArray();

					packet.setData(data);
					socket.send(packet);
				} catch (IOException e) {
					System.err.println("Problemi nell'invio della risposta: " + e.getMessage());
					e.printStackTrace();
					continue;

				}

			} // while

		}

		catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Server: termino...");
		socket.close();
	}

}
