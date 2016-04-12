package sensor.base;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SensorStateChangeListener extends Remote{
	public void onStateChange(SensorState from, SensorState to) throws RemoteException;
}
