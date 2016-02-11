package sensor;

import java.rmi.RemoteException;

public interface TempSensor extends Sensor {
	public enum Unit {
		CELSIUS, FAHRENHEIT, KELVIN;
		public static Double convert(Double value, Unit from, Unit to) {
			switch (from) {
			case CELSIUS:
				switch (to) {
				case CELSIUS:
					return value;
				case KELVIN:
					return value + 273.15;
				case FAHRENHEIT:
					return value * 9.0/5.0 + 32;
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
	 * @return a {@link FutureResult} representing the temperature that will be read
	 */
	public FutureResult<Double> readTemperatureAsync(Unit unit) throws RemoteException;
}
