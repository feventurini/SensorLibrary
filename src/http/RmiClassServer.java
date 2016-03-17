package http;

import java.io.IOException;
import java.util.concurrent.Executors;

/**
 * Serves classes from the current classloader to remote clients for RMI. The
 * server should be started before any remote objects are bound.
 */
public class RmiClassServer {

	// private final String serverHostname;
	private final MiniHttpServer httpServer;

	/**
	 * @param serverHostname
	 *            the local machine's hostname that remote clients can use to
	 *            address this host.
	 */
	public RmiClassServer() {
		httpServer = new MiniHttpServer(Executors.newFixedThreadPool(3), url -> {
			System.out.println("RmiClassServer richiesto url: " + url);
			return getClass().getResourceAsStream(url);
		});
	}

	/**
	 * Restituisce sempre 0.0.0.0 perchè la ServerSocket del server è in ascolto
	 * su qualsiasi indirizzo
	 *
	 * @return
	 */
	public String getAddress() {
		return httpServer.getAddress();
	}

	/**
	 * Returns the port that this server is listening for connections on.
	 */
	public int getHttpPort() {
		return httpServer.getHttpPort();
	}

	public String getUrl() {
		return "http://" + httpServer.getAddress() + ":" + httpServer.getHttpPort();
	}

	/**
	 * Startup the server and handle requests indefinitely. This method returns
	 * once the server is ready.
	 */
	public void start() {
		try {
			httpServer.start();
			System.out.println("RmiClassServer started on: " + getUrl());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
