package provider;

import java.rmi.Remote;
import java.rmi.RemoteException;

import sensor.base.Sensor;
import station.Station;

public interface RegistrationListener extends Remote {
	public void onStationRegistered(String stationName, Station station) throws RemoteException;
	public void onStationUnRegistered(String stationName, Station station) throws RemoteException;
	public void onSensorRegistered(SensorId fullName, Sensor sensor) throws RemoteException;
	public void onSensorUnRegistered(SensorId fullName, Sensor sensor) throws RemoteException;
}
