package sensor;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class FutureResultImpl<T> extends UnicastRemoteObject implements FutureResult<T> {
	private static final long serialVersionUID = 1L;
	private Exception e = null;
	private T result = null;

	public FutureResultImpl() throws RemoteException {
		super();
	}


	public synchronized void set(T result) {
		this.result = result;
		this.notify();
	}

	public synchronized void raiseException(Exception e) {
		this.e = e;
		this.notify();
	}

	public synchronized T get() throws RemoteException {
		while (result == null && e == null)
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		if (e != null)
			throw new RemoteException(e.getMessage(), e);
		else
			return result;
	}
}
