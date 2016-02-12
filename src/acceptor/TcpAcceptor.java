package acceptor;

import java.io.IOException;
import java.net.ServerSocket;
import comm.Communicator;
import comm.RemoteCommunicator;

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
