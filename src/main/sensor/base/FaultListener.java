package sensor.base;

public abstract class FaultListener implements SensorStateChangeListener {

	@Override
	public final void onStateChange(SensorState from, SensorState to) {
		onFault();
	}

	public abstract void onFault();
}
