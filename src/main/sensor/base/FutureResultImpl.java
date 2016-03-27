package sensor.base;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of the interface {@link FutureResult}. It is meant to be
 * used server-side only as it contains methods to set the result or raise an
 * exception.
 *
 * @param <T>
 *            the class representing the result
 */
public class FutureResultImpl<T> extends UnicastRemoteObject implements FutureResult<T> {
	private static final long serialVersionUID = 9110124935596402340L;
	private final static Logger log = Logger.getLogger(FutureResultImpl.class.getName());
	private Exception e = null;
	private T result = null;

	public FutureResultImpl() throws RemoteException {
		super();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see sensor.FutureResult#get()
	 */
	@Override
	public synchronized T get() throws RemoteException {
		while (result == null && e == null) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				log.log(Level.WARNING, "Future result interrupted", e);
				throw new RemoteException(e.getMessage(), e);
			}
		}
		if (e != null)
			throw new RemoteException(e.getMessage(), e);
		else
			return result;
	}

	/**
	 * If an error occurs during the remote operation a description of the error
	 * should be sent to the client by means of an {@link Exception}. When the
	 * client will call the {@link #get()} method a {@link RemoteException} will
	 * be thrown with the original exception as the cause.
	 *
	 * @param e
	 */
	public synchronized void raiseException(Exception e) {
		this.e = e;
		this.notify();
	}

	/**
	 * Use this method to set the result of a remote operation. A client blocked
	 * on the {@link #get()} method will be notified and any successive call
	 * will return immediately.
	 *
	 * @param result
	 */
	public synchronized void set(T result) {
		this.result = result;
		this.notify();
	}
}
