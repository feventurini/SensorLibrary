package discovery;

import java.rmi.Remote;
import java.rmi.RemoteException;

import sensor.Sensor;

public interface Provider extends Remote {

	/**
	 * If possible finds the sensor registered with the name and location
	 * provided
	 * 
	 * @param location
	 *            the location
	 * @param name
	 *            the name
	 * @return a remote reference to the sensor
	 * @throws RemoteException
	 *             if the sensor were not found
	 */
	public Sensor find(String location, String name) throws RemoteException;

	/**
	 * If possible finds the all the sensors registered with the name and
	 * location provided
	 * 
	 * @param location
	 *            the location (null for any location)
	 * @param name
	 *            the name (null for any name)
	 * @return an array of remote references to the sensors, possibly with 0
	 *         length
	 * @throws RemoteException
	 */
	public Sensor[] findAll(String location, String name) throws RemoteException;

	/**
	 * Register a new Sensor
	 * 
	 * @param location
	 *            the location
	 * @param name
	 *            the name
	 * @param sensor
	 *            the sensor itself
	 * @throws RemoteException
	 *             if the registration was not possible
	 */
	public void register(String location, String name, Sensor sensor) throws RemoteException;

	/**
	 * Unregister a sensor
	 * 
	 * @param location
	 *            the location
	 * @param name
	 *            the name
	 * @throws RemoteException
	 *             if the unregistration was not possible
	 */
	public void unregister(String location, String name) throws RemoteException;
}
