package comm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


public class LocalCommunicator implements Communicator {
	private OutputStream commandStream;
	private InputStream dataStream;
	private FileChannel fileChannel;
	
	public LocalCommunicator(String sensorName) {
		File file = new File(sensorName);
        try {
        	// Get file channel in readwrite mode
			fileChannel = new RandomAccessFile(file, "rw").getChannel();
 
			// Get direct byte buffer access using channel.map() operation
			MappedByteBuffer commandBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, 1024*1024);
			MappedByteBuffer dataBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 1024*1024, 1024*1024);

			commandStream = new ByteBufferOutputStream(commandBuffer);
			dataStream = new ByteBufferInputStream(dataBuffer);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void close() throws IOException {
		fileChannel.close();
		commandStream.close();
		dataStream.close();	
	}

	@Override
	public InputStream getDataStream() {
		return dataStream;
	}

	@Override
	public OutputStream getCommandStream() {
		return commandStream;
	}

}
