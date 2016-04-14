package sensor.base;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public abstract class StateListenerImpl extends UnicastRemoteObject implements StateListener {
	private static final long serialVersionUID = 941117060315216159L;
	
	protected StateListenerImpl() throws RemoteException {
		super();
	}


	public abstract void onStateChange(Sensor sensor, SensorState from, SensorState to) throws RemoteException;

}
