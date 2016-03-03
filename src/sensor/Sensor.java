package sensor;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Sensor extends Remote {
	public SensorState getState() throws RemoteException;
}
