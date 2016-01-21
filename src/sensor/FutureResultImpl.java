package sensor;

import java.rmi.RemoteException;

public class FutureResultImpl<T> implements FutureResult<T> {	
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
