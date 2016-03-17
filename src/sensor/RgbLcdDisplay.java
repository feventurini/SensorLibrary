package sensor;

import java.rmi.RemoteException;

/**
 * The public interface of an rgb display. Contains methods to set the rgb color
 * of the display and to display a string.
 */
public interface RgbLcdDisplay extends Sensor {

	/**
	 * Display a text for a given time. If time is 0, the text is displayed
	 * indefinitely.
	 *
	 * @param text
	 *            the text to display
	 * @param time
	 *            how long the text will be displayed, 0 for displaying until
	 *            further change
	 * @throws RemoteException
	 */
	public void display(String text, int time) throws RemoteException;

	/**
	 * Set RGB the color to display
	 * 
	 * @param r
	 * @param g
	 * @param b
	 * @throws RemoteException
	 */
	public void setRGB(int r, int g, int b) throws RemoteException;

}
