package sensor;

/**
 * All sensors must be able to provide information about their state by means of
 * {@link Sensor#getState()}. A sensor state is represented by one of the
 * following states: {@link RUNNING},
 * {@link State#FAULT}, {@link SHUTDOWN}. Moreover a string can be set on
 * the SensorState to provide additional information.
 */
public enum SensorState {
	RUNNING, FAULT, SHUTDOWN;
}
