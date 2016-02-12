package server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import acceptor.Acceptor;
import comm.Communicator;

public abstract class SensorServer {

	ArrayList<Acceptor> acceptors;
	String sensorName;

	public SensorServer(String sensorName) {
		this.sensorName = sensorName;
		acceptors = new ArrayList<>();
	}

	public abstract void addCommunicator(Communicator comm);

	protected class AcceptThread extends Thread {
		private Acceptor acc;
		private SensorServer server;

		public AcceptThread(Acceptor acc, SensorServer server) {
			this.acc = acc;
			this.server = server;
		}

		public void run() {
			while (true) {
				try {
					server.addCommunicator(acc.accept());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void startup() {
		acceptors.forEach((a) -> new AcceptThread(a, this).start());
	}

	// non facciamo un metodo statico nel discovery per la registrazione in modo
	// tale da non doverne conoscere il .class ma solamente l'indirizzo di rete
	public void registerOnDiscovery(int sensorPort, InetSocketAddress discoveryAddress) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		char operation = 's';
		dos.writeChar(operation);
		dos.writeInt(sensorPort);
		dos.writeUTF(sensorName);
		byte data[] = bos.toByteArray();
		DatagramPacket packet = new DatagramPacket(data, data.length, discoveryAddress);
		DatagramSocket socket = new DatagramSocket();
		socket.send(packet);

		data = new byte[256];
		packet.setData(data);
		socket.receive(packet);
		ByteArrayInputStream bis = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
		DataInputStream dis = new DataInputStream(bis);
		
		char result = dis.readChar();
		if (result=='n') {
			System.out.println("Error during registration on discovery");
			socket.close();
			throw new IOException();
		}
		
		socket.close();
	}
}
