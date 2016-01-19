package sensor;

import java.rmi.RemoteException;
import java.util.concurrent.Future;

public interface RfidSensor extends Sensor {
	/**
	 * Reads a tag synchronously
	 * 
	 * @return the tag read, null if not present
	 */
	//TODO: returns null or exception?
	public String readTag() throws RemoteException;

	/**
	 * Reads a tag asynchronously
	 * 
	 * @return a {@link Future} representing the tag that will be read when present
	 */
	public Future<Double> readTemperatureAsync() throws RemoteException;
}
