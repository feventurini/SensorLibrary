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

  /**
   * @param serverHostname the local machine's hostname that remote clients can
   *      use to address this host.
   */
  public RmiClassServer(String serverHostname) {
    this.serverHostname = serverHostname;
  }

  /**
   * Startup the server and handle requests indefinitely. This method returns
   * once the server is ready.
   */
  public void run() {
    MiniHttpServer httpServer = new MiniHttpServer(
        Executors.newFixedThreadPool(3),
        new MiniHttpServer.Handler() {
          public InputStream getResponse(String url) throws IOException {
            return getClass().getResourceAsStream(url);
          }
        });
    
    try {
      httpServer.run();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    System.setProperty("java.rmi.server.codebase",
        "http://" + serverHostname + ":" + httpServer.getHttpPort() + "/");
  }

}

