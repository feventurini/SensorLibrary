package sensor.interfaces;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Calendar;

import sensor.base.FutureResult;
import sensor.base.Sensor;

public interface AstronomySensor extends Sensor {
	public class Astronomy implements Serializable {
		private static final long serialVersionUID = -872271175748776900L;
		public Calendar sunrise;
		public Calendar sunset;
		public Calendar moonrise;
		public Calendar moonset;
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "Astronomy [sunrise=" + sunrise + ", sunset=" + sunset + ", moonrise=" + moonrise + ", moonset="
					+ moonset + "]";
		}
	}
	
	public Astronomy getAstronomy() throws RemoteException;

	public FutureResult<Astronomy> getAstronomyAsync() throws RemoteException;	
}
