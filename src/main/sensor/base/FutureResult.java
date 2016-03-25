package sensor.base;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * This interface allows a client to receive the result of an operation on the
 * remote sensor. The result can be of any kind, as long as it can be
 * serialized. Calling the method {@link #get()} will return immediately if the
 * result is ready, or block otherwise. If an exception has been raised by the
 * remote sensor it will be set as the cause of the {@link RemoteException}
 * thrown by {@link #get()}
 * 
 * @param <T> the class representing the result
 */
public interface FutureResult<T> extends Remote {
	/**
	 * This method will return immediately if the result of the operation is
	 * ready, or block otherwise. If an exception has been raised by the remote
	 * sensor it will be set as the cause of the {@link RemoteException}.
	 * 
	 * @return the result
	 * @throws RemoteException if an error occurs within the remote sensor
	 */
	public T get() throws RemoteException;
}
