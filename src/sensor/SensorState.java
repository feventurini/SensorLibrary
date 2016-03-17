package sensor;

import java.io.Serializable;

/**
 * All sensors must be able to provide information about their state by means of
 * {@link Sensor#getState()}. A sensor state is represented by one of the
 * following states: {@link State#SETUP}, {@link State#RUNNING},
 * {@link State#MEASURING}, {@link State#FAULT}, {@link State#SHUTDOWN}.
 * Moreover a string can be set on the SensorState to provide additional
 * information.
 */
public class SensorState implements Serializable {
	public enum State implements Serializable {
		SETUP, RUNNING, MEASURING, FAULT, SHUTDOWN
	}

	private static final long serialVersionUID = 2419411720859312714L;

	private String comment;
	private State state;

	public SensorState(State state, String comment) {
		this.state = state;
		this.comment = comment;
	}

	/**
	 * @return the comment if any, an empty string otherwise
	 */
	public synchronized String getComment() {
		return comment;
	}

	/**
	 * @return the state
	 */
	public synchronized State getState() {
		return state;
	}

	/**
	 * Sets the comment of the SensorState, calling {@link #setState(State)}
	 * will erase the prevoius comment. So this method must be called after
	 * setting the state.
	 * 
	 * @param comment
	 *            the comment to set
	 */
	public synchronized void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * Sets the state of the sensor, also erasing the previus comment.
	 * 
	 * @param state
	 *            the state to set
	 */
	public synchronized void setState(State state) {
		this.state = state;
		this.comment = "";
	}

}
