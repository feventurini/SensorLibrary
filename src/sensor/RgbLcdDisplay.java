package sensor;

import java.rmi.RemoteException;

public interface RgbLcdDisplay extends Sensor {

	/**
	 * Display a text for a given time. If time is 0, the text is displayed
	 * until it is changed
	 * 
	 * @param text
	 *            the text to display
	 * @param time
	 *            how long the text will be displayed, 0 for displaying until
	 *            further changing
	 * @throws RemoteException
	 */
	public void display(String text, int time) throws RemoteException;
	
	/**
	 * Set RGB color to the display
	 * @param r
	 * @param g
	 * @param b
	 * @throws RemoteException
	 */
	public void setRGB(int r, int g, int b) throws RemoteException;

}
