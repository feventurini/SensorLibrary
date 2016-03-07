package sensor;

import java.rmi.RemoteException;

public interface HumiditySensor extends Sensor {
	/**
	 * Reads the humidity synchronously
	 *
	 * @return the humidity read
	 */
	public Double readHumidity() throws RemoteException;

	/**
	 * Reads the humidity asynchronously
	 *
	 * @return a {@link FutureResult} representing the humidity that will be
	 *         read
	 */
	public FutureResult<Double> readHumidityAsync() throws RemoteException;

}
