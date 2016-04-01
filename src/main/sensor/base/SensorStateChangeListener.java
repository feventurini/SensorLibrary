package sensor.base;

public interface SensorStateChangeListener {
	public void onStateChange(SensorState from, SensorState to);
}
