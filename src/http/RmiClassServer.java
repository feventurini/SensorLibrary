package http;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;

/**
 * Serves classes from the current classloader to remote clients for RMI. The
 * server should be started before any remote objects are bound.
 *
 * @author jessewilson
 */
public class RmiClassServer {

	private final String serverHostname;
	private final MiniHttpServer httpServer;

	/**
	 * @param serverHostname
	 *            the local machine's hostname that remote clients can use to
	 *            address this host.
	 */
	public RmiClassServer(String serverHostname) {
		this.serverHostname = serverHostname;
		httpServer = new MiniHttpServer(Executors.newFixedThreadPool(3), new MiniHttpServer.Handler() {
			public InputStream getResponse(String url) throws IOException {
				System.out.println("RmiClassServer: richiesto url " + url);
				return getClass().getResourceAsStream(url);
			}
		});
	}

	/**
	 * Startup the server and handle requests indefinitely. This method returns
	 * once the server is ready.
	 */
	public void start() {
		try {
			httpServer.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String getFullName() {
		return "http://" + serverHostname + ":" + httpServer.getHttpPort();
	}
}
