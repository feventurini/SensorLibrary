package acceptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import comm.Communicator;
import comm.LocalCommunicator;

public class MmfAcceptor implements Acceptor {

	// Usiamo blocking Queue con i metodi put e take che fanno attendere il
	// thread in caso di coda piena e vuota

	private Path dir;
	private BlockingQueue<Communicator> queue;
	private final AtomicBoolean failure;

	public MmfAcceptor(String dirPath) throws IOException {
		this.dir = Paths.get(dirPath);
		this.queue = new ArrayBlockingQueue<>(10);
		this.failure = new AtomicBoolean(false);

		new MmfAcceptorThread(dir, queue).start();
	}

	@Override
	public Communicator accept() throws IOException {
		if (failure.get()) 
			throw new IOException("Directory failure " + dir);
		try {
			return queue.take();
		} catch (InterruptedException e) {
			throw new IOException();
		}
	}

	public class MmfAcceptorThread extends Thread {
		private Path dir;
		private BlockingQueue<Communicator> queue;

		public MmfAcceptorThread(Path dir, BlockingQueue<Communicator> queue) {
			this.dir = dir;
			this.queue = queue;
		}

		public void run() {
			WatchService watcher = null;
			try {
				watcher = dir.getFileSystem().newWatchService();
				dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
			} catch (IOException e) {
				e.printStackTrace();
				failure.set(true);
				return;
			}

			while (true) {
				try {
					WatchKey watchKey = watcher.take();

					List<WatchEvent<?>> events = watchKey.pollEvents();
					for (WatchEvent<?> event : events) {
						if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
							queue.put(new LocalCommunicator(new File(event.context().toString().trim())));
						}
					}

					boolean valid = watchKey.reset();
					if (!valid) {
						failure.set(true);
						return;
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
	}

}
