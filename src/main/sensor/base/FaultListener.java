package sensor.base;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public abstract class FaultListener  extends UnicastRemoteObject implements SensorStateChangeListener {
	protected FaultListener() throws RemoteException {
		super();
	}

	private static final long serialVersionUID = -8904519187773473558L;

	@Override
	public final void onStateChange(SensorState from, SensorState to) throws RemoteException {
		onFault();
	}

	public abstract void onFault();
}
