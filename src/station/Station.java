package station;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import javax.annotation.Nullable;

import sensor.SensorState;

public interface Station extends Remote {
	/**
	 * Returns a list of all available sensors on the station, filtered basing
	 * on the state passed as parameter
	 * 
	 * @param state
	 *            (null for any state)
	 * @return a list of the logic names of the sensors
	 * @throws RemoteException
	 */
	public List<String> listSensors(@Nullable SensorState state) throws RemoteException;

	/**
	 * Returns the state of the sensor identified by the name passed as
	 * parameter
	 * 
	 * @param name
	 * @return the state of the sensor
	 * @throws RemoteException if the name is not found
	 */
	public SensorState getSensorState(String name) throws RemoteException;
	
	public void startSensor(String name) throws RemoteException;
	
	public void stopSensor(String name) throws RemoteException;
}
