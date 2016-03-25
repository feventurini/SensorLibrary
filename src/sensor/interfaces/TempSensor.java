package sensor.interfaces;

import java.rmi.RemoteException;

import sensor.base.FutureResult;
import sensor.base.Sensor;

/**
 * The public interface of a temperature sensor. Contains methods to read the
 * temperature, both asynchronously and synchronously.
 */
public interface TempSensor extends Sensor {
	public enum Unit {
		CELSIUS, FAHRENHEIT, KELVIN;
		/**Convenient method to convert a temperature between a unit and another
		 * @param value the value
		 * @param from the unit to convert from
		 * @param to the unit to convert to
		 * @return
		 */
		public static Double convert(Double value, Unit from, Unit to) {
			switch (from) {
			case CELSIUS:
				switch (to) {
				case CELSIUS:
					return value;
				case KELVIN:
					return value + 273.15;
				case FAHRENHEIT:
					return value * 9.0 / 5.0 + 32;
				}
			case KELVIN:
				switch (to) {
				case CELSIUS:
					return value - 273.15;
				case KELVIN:
					return value;
				case FAHRENHEIT:
					return (value - 273.15 - 32) * 5.0 / 9.0;
				}
			case FAHRENHEIT:
				switch (to) {
				case CELSIUS:
					return (value - 32) * 5.0 / 9.0;
				case KELVIN:
					return (value - 32) * 5.0 / 9.0 + 273.15;
				case FAHRENHEIT:
					return value;
				}
			}
			return null; // never reached
		}
	}

	/**
	 * Reads the temperature synchronously
	 *
	 * @return the temperature read
	 */
	public Double readTemperature(Unit unit) throws RemoteException;

	/**
	 * Reads the temperature asynchronously
	 *
	 * @return a {@link FutureResult} representing the temperature that will be
	 *         read
	 */
	public FutureResult<Double> readTemperatureAsync(Unit unit) throws RemoteException;
}
