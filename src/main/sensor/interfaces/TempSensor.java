package sensor.interfaces;

import java.rmi.RemoteException;

import sensor.base.FutureResult;
import sensor.base.Sensor;

/**
 * The public interface of a temperature sensor. Contains methods to read the
 * temperature, both asynchronously and synchronously.
 */
public interface TempSensor extends Sensor {
	/**
	 * Reads the temperature synchronously
	 *
	 * @return the temperature read
	 */
	public Double readTemperature() throws RemoteException;

	/**
	 * Reads the temperature asynchronously
	 *
	 * @return a {@link FutureResult} representing the temperature that will be
	 *         read
	 */
	public FutureResult<Double> readTemperatureAsync() throws RemoteException;
}
