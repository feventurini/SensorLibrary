package sensor;

import java.io.Serializable;

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
	 * @return the comment
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
	 * @param comment
	 *            the comment to set
	 */
	public synchronized void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @param state
	 *            the state to set
	 */
	public synchronized void setState(State state) {
		this.state = state;
	}

}
