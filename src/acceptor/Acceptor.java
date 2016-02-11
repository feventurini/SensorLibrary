package acceptor;

import java.io.IOException;

import comm.Communicator;

public interface Acceptor{
	public Communicator accept() throws IOException;
}
