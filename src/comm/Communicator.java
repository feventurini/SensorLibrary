package comm;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;


public interface Communicator extends Closeable{
	public InputStream getDataStream();
	public OutputStream getCommandStream();
}
