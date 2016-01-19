package discovery;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import sensor.Sensor;

public class RMI_Server extends UnicastRemoteObject implements
    Discovery {
  private static final long serialVersionUID = 1L;

  public RMI_Server() throws RemoteException {
    super();
  }

  public synchronized void metodo1() throws RemoteException {
  }

  public synchronized String[] metodo2(int a, String b) throws RemoteException{
    return null;
  }

  // Avvio del Server RMI
  public static void main(String[] args) {
    int registryPort = 1099;
    String registryHost = "localhost";
    String serviceName = "Server";

    if (args.length == 1) {
      try {
        registryPort = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        System.out.println("Usage: RMI_Server [registryPort]");
        System.exit(1);
      }
      if (registryPort < 1024 || registryPort > 65535) {
        System.out.println("Port out of range");
        System.exit(2);
      }
    } else if (args.length>1){
    	System.out.println("Too many arguments");
    	System.out.println("Usage: RMI_Server [registryPort]");
    	System.exit(-1);
    }
    
    // Eventuale inizializzazione dello stato
    
    // Registrazione del servizio RMI
    String completeName = "//" + registryHost + ":" + registryPort + "/"
        + serviceName;
    try{
      RMI_Server serverRMI = new RMI_Server();
      Naming.rebind(completeName, serverRMI);
      System.out.println("Server RMI: Servizio " + serviceName
					+ " registrato");
    } catch (RemoteException | MalformedURLException e) {
      e.printStackTrace();
      System.exit(3);
    }
  }

@Override
public Sensor find(String location, String name) throws RemoteException {
	// TODO Auto-generated method stub
	return null;
}
}
