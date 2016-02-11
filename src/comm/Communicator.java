package comm;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public interface Communicator extends Closeable {
	public InputStream getInputStream() throws IOException;
	public OutputStream getOutputStream() throws IOException;
}
