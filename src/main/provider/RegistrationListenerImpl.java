package provider;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import sensor.base.Sensor;
import station.Station;

public abstract class RegistrationListenerImpl extends UnicastRemoteObject implements RegistrationListener {
	private static final long serialVersionUID = 4702919144811771972L;
	protected RegistrationListenerImpl() throws RemoteException {
		super();
	}
	public abstract void onStationRegistered(String stationName, Station station) throws RemoteException;
	public abstract void onStationUnRegistered(String stationName) throws RemoteException;
	public abstract void onSensorRegistered(SensorId fullName, Sensor sensor) throws RemoteException;
	public abstract void onSensorUnRegistered(SensorId fullName) throws RemoteException;
}
