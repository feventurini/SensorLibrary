package sensor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import comm.Communicator;
import comm.RemoteCommunicator;

public abstract class SensorServer {

	public abstract void addCommunicator(Communicator comm);

}
