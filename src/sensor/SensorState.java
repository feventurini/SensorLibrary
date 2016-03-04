package sensor;

import java.io.Serializable;

public class SensorState implements Serializable{
	private static final long serialVersionUID = 2419411720859312714L;

	public enum State implements Serializable {
		SETUP,
		RUNNING,
		MEASURING,
		FAULT,
		SHUTDOWN
	}

	private String comment;
	private State state;
	
	public SensorState(State state, String comment){
		this.state = state;
		this.comment = comment;
	}
	
	/**
	 * @param state the state to set
	 */
	public synchronized void setState(State state) {
		this.state = state;
	}

	/**
	 * @param comment the comment to set
	 */
	public synchronized void setComment(String comment) {
		this.comment = comment;
	}


	/**
	 * @return the state
	 */
	public synchronized State getState() {
		return state;
	}

	/**
	 * @return the comment
	 */
	public synchronized String getComment() {
		return comment;
	}
	
	
}
