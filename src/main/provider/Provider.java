package provider;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import sensor.base.Sensor;
import station.Station;

/**
 * The public interface of a Provider. Contains methods to build the provider
 * rmi url from its hostname (and port), to find the provider over the network
 * using datagram packets sent to a multicast group. Moreover defines the public
 * services that the provider offers: finding a sensor given its name and
 * position, finding all the sensors matching a given name or location,
 * registering and unregistering a sensor.
 */
public interface Provider extends Remote {
	public static final String PROVIDER_NAME = "ProviderRMI";
	public static final int PROVIDER_PORT = 1099;

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
	 * @return an list of remote references to the sensors, possibly with 0
	 *         length
	 * @throws RemoteException
	 */
	public List<Sensor> findAll(String location, String name) throws RemoteException;

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

	public void registerStation(String stationName, Station station) throws RemoteException;

	/**
	 * Unregister a Sensor
	 *
	 * @param location
	 *            the location
	 * @param name
	 *            the name
	 * @throws RemoteException
	 *             if the unregistration was not possible
	 */
	public void unregister(String location, String name) throws RemoteException;

	public void unregisterStation(String stationName) throws RemoteException;

}
