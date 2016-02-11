package comm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


public class LocalCommunicator implements Communicator {
	private OutputStream outputStream;
	private InputStream inputStream;
	private FileChannel fileChannel;
	private File file;
	
	public LocalCommunicator(File file) {
        try {
        	this.file = file;
        	// Get file channel in readwrite mode
			fileChannel = new RandomAccessFile(file, "rw").getChannel();
 
			// Get direct byte buffer access using channel.map() operation
			// 2 MB memory mapped files, the first 1024 bytes used 
			MappedByteBuffer inputBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, 1024*1024);
			MappedByteBuffer outputBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 1024*1024, 1024*1024);

			outputStream = new ByteBufferOutputStream(inputBuffer);
			inputStream = new ByteBufferInputStream(outputBuffer);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void close() throws IOException {
		fileChannel.close();
		outputStream.close();
		inputStream.close();	
		file.delete();
	}

	@Override
	public InputStream getInputStream() {
		return inputStream;
	}

	@Override
	public OutputStream getOutputStream() {
		return outputStream;
	}

}
