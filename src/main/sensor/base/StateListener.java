package sensor.base;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface StateListener extends Remote{
	public void onStateChange(Sensor sensor, SensorState from, SensorState to) throws RemoteException;
}
