package sensor.interfaces;

import java.io.Serializable;
import java.rmi.RemoteException;

import sensor.base.FutureResult;
import sensor.base.Sensor;

public interface WeatherSensor extends Sensor {
	public class Observation implements Serializable {
		private static final long serialVersionUID = 1234567890L;
		public Double temp;
		public Double feelsLike;
		public Double windSpeed;
		public Double windDegrees;
		public Double pressure;
		public Double mmRain;

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "Observation [temp=" + temp + ", feelsLike=" + feelsLike + ", windSpeed=" + windSpeed
					+ ", windDegrees=" + windDegrees + ", pressure=" + pressure + ", mmRain=" + mmRain + "]";
		}
	}

	public Observation getObservation() throws RemoteException;

	public FutureResult<Observation> getObservationAsync() throws RemoteException;
}
