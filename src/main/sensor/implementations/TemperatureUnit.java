package sensor.implementations;

public enum TemperatureUnit {
	CELSIUS, FAHRENHEIT, KELVIN;
	/**
	 * Convenient method to convert a temperature between a unit and another
	 *
	 * @param value
	 *            the value
	 * @param from
	 *            the unit to convert from
	 * @param to
	 *            the unit to convert to
	 * @return
	 */
	public static Double convert(Double value, TemperatureUnit from, TemperatureUnit to) {
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