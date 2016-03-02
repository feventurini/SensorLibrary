package http;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A very small HTTP server, designed to support only a single client, the Java
 * RMI URL Classloader.
 *
 * @author jessewilson
 */
public class MiniHttpServer {
  private static final Logger LOGGER = Logger.getLogger(MiniHttpServer.class.getName());

  private final ExecutorService executorService;
  private final Handler handler;
  private ServerSocket serverSocket;

  public MiniHttpServer(ExecutorService executorService, Handler handler) {
    this.executorService = executorService;
    this.handler = handler;
  }

  /**
   * Startup the server and handle requests indefinitely. This method returns
   * after the server has been bound but before any requests have been served.
   */
  void start() throws IOException {
    checkState(this.serverSocket == null);
    this.serverSocket = new ServerSocket(0);

    executorService.execute(new Runnable() {
      public void run() {
        try {
          while (true) {
            executorService.execute(new ConnectionHandler(serverSocket.accept()));
          }
        } catch (IOException e) {
          LOGGER.warning(e.getMessage());
        }
      }
    });
  }

  /**
   * Lightweight but naive implementation of HTTP 1.1.
   */
  private class ConnectionHandler implements Runnable {
    private final Socket connection;
    private final BufferedReader reader;
    private String currentLine;

    private ConnectionHandler(Socket connection) throws IOException {
      this.connection = connection;
      this.reader = new BufferedReader(new InputStreamReader(
          connection.getInputStream(), "ISO-8859-1"));
    }

    public void run() {
      try {
        nextLine();
        readUntil("GET\\s+");
        String url = readUntil("\\s+HTTP/1.1");

        do {
          nextLine();
        } while (readUntil("$").length() > 0);

        InputStream responseData = handler.getResponse(url);
        if (responseData != null) {
          write("HTTP/1.1 200 OK\r\n\r\n");
          int b;
          // TODO migliorare l'efficienza
          while ((b = responseData.read()) != -1) {
            connection.getOutputStream().write(b);
          }

        } else {
          write("HTTP/1.1 404 Not Found\r\n\r\n");
          write("404 Not Found: " + url);

        }

        connection.close();
      } catch (IOException e) {
        LOGGER.info(e.getMessage());
      }
    }

    private void write(String data) throws IOException {
      connection.getOutputStream().write(data.getBytes("ISO-8859-1"));
    }

    String readUntil(String pattern) throws IOException {
      Matcher matcher = Pattern.compile(pattern).matcher(currentLine);
      checkState(matcher.find());
      String found = currentLine.substring(0, matcher.start());
      currentLine = currentLine.substring(matcher.end());
      return found;
    }

    void nextLine() throws IOException {
      currentLine = reader.readLine();
    }
  }
  
  private void checkState(boolean condition) {
    if (!condition) {
      throw new IllegalStateException();
    }
  }

  /**
   * Handler for incoming HTTP requests.
   */
  public interface Handler {

    /**
     * Responds to an HTTP request for the specified url.
     *
     * @param url a non-null URL-encoded String starting with "/".
     * @return an InputStream that the response data can be read from, or
     *      {@code null} if there is no data at {@code url}.
     */
    InputStream getResponse(String url) throws IOException;
  }

  public static void main(String[] args) throws IOException {
    Handler handler = new Handler() {
      public InputStream getResponse(String url) throws IOException {
        return new ByteArrayInputStream(("You requested " + url).getBytes("ISO-8859-1"));
      }
    };

    MiniHttpServer server = new MiniHttpServer(Executors.newFixedThreadPool(3), handler);
    server.start();
    System.out.println("MiniHttpServer started on: http://" + server.getAddress() + ":" + server.getHttpPort() + "/");
  }

	public String getAddress() {
		return serverSocket.getInetAddress().getHostAddress();
	}

	/**
	 * Returns the port that this server is listening for connections on.
	 */
	public int getHttpPort() {
		checkState(serverSocket != null);
		return serverSocket.getLocalPort();
	}
}
