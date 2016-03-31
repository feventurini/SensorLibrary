package provider;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

import sensor.base.Sensor;
import station.Station;

/**
 * The public interface of a Provider. Defines the public services that the
 * provider offers: finding a sensor given its name and position, finding all
 * the sensors matching a given name or location, registering and unregistering
 * a sensor.
 */
public interface Provider extends Remote {
	/**
	 * If possible finds the sensor registered with the name and location
	 * provided
	 * 
	 * @param name
	 *            the name
	 * @param location
	 *            the location
	 *
	 * @return a remote reference to the sensor
	 * @throws RemoteException
	 *             if the sensor was not found
	 */
	public Sensor find(String name, String location) throws RemoteException;

	/**
	 * If possible finds the station registered on the location provided
	 *
	 * @param location
	 *            the location
	 * @return a remote reference to the station
	 * @throws RemoteException
	 *             if the station was not found
	 */
	public Station findStation(String location) throws RemoteException;

	/**
	 * Finds the all the sensors registered with filtered by name (if provided),
	 * by location (if provided) or by an interface (if provided)
	 * 
	 * @param name
	 *            the name (null for any name)
	 * @param location
	 *            the location (null for any location)
	 * @param type
	 *            the interface (null for any interface)
	 * @return an list of remote references to the sensors, possibly with 0
	 *         length
	 * @throws RemoteException
	 */
	public Map<String, Sensor> findAll(String name, String location, Class<? extends Sensor> type) throws RemoteException;

	/**
	 * Register a new Sensor
	 * 
	 * @param name
	 *            the name
	 * @param location
	 *            the location
	 * @param sensor
	 *            the sensor itself
	 *
	 * @throws RemoteException
	 *             if the registration was not possible
	 */
	public void register(String name, String location, Sensor sensor) throws RemoteException;

	public void registerStation(String stationName, Station station) throws RemoteException;

	/**
	 * Unregister a Sensor
	 * 
	 * @param name
	 *            the name
	 * @param location
	 *            the location
	 *
	 * @throws RemoteException
	 *             if the unregistration was not possible
	 */
	public void unregister(String name, String location) throws RemoteException;

	public void unregisterStation(String stationName) throws RemoteException;

}
