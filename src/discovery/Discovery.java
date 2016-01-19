package discovery;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

import sensor.Sensor;

public interface Discovery extends Remote {

	/**If possible finds the sensor registered with the name and location provided
	 * @param location the location
	 * @param name the name
	 * @return a remote reference to the sensor
	 * @throws RemoteException
	 */
	public Sensor find(String location, String name) throws RemoteException;
	
	public void metodo1() throws RemoteException;
	public String[] metodo2(int a, String b) throws RemoteException;

}
