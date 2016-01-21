package sensor;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FutureResult<T> extends Remote {
	public T get() throws RemoteException;	
}
