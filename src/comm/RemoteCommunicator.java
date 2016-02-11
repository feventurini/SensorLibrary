package comm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class RemoteCommunicator implements Communicator {
	
	private Socket socket;

	public RemoteCommunicator(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void close() throws IOException {
		socket.close();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return socket.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}

}
