package acceptor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.activation.CommandInfo;

import comm.Communicator;
import comm.RemoteCommunicator;
import sensor.SensorServer;

public class TcpAcceptor implements Acceptor {
	
	private ServerSocket socket;

	public TcpAcceptor(ServerSocket socket) {
		this.socket = socket;
	}

	@Override
	public Communicator accept() throws IOException {
		return new RemoteCommunicator(socket.accept());
	}
	

}
