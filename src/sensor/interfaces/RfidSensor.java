package sensor.interfaces;

import java.rmi.RemoteException;

import sensor.FutureResult;
import sensor.Sensor;

/**
 * The public interface of an Rfid sensor. Contains methods to read a tag, both
 * asynchronously and synchronously.
 */
public interface RfidSensor extends Sensor {
	/**
	 * Reads a tag synchronously
	 *
	 * @return the tag read
	 */
	// TODO: returns null or exception?
	public String readTag() throws RemoteException;

	/**
	 * Reads a tag asynchronously
	 *
	 * @return a {@link FutureResult} representing the tag that will be read when
	 *         available
	 */
	public FutureResult<String> readTagAsync() throws RemoteException;
}
