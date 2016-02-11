package sensor;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class FutureResultImpl<T> extends UnicastRemoteObject implements FutureResult<T> {	
	private static final long serialVersionUID = 1L;

	public FutureResultImpl() throws RemoteException {
		super();
	}

	private T result = null;
	
	public synchronized void set(T result) {
		this.result = result;
		this.notify();
	}
	
	public synchronized T get() throws RemoteException {
		while (result==null)
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		return result;
	}
}
