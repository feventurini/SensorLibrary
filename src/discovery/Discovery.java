package discovery;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Discovery extends Remote {

	
	
	public void metodo1() throws RemoteException;
	public String[] metodo2(int a, String b) throws RemoteException;

}
