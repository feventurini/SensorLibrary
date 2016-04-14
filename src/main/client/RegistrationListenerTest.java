package client;

import java.rmi.RemoteException;

import provider.RegistrationListener;
import provider.SensorId;
import sensor.base.Sensor;
import station.Station;

public class RegistrationListenerTest implements RegistrationListener  {
	
	@Override
	public void onStationUnRegistered(String stationName, Station station) throws RemoteException {
		System.out.println("Unregistered " + stationName);
	}
	
	@Override
	public void onStationRegistered(String stationName, Station station) throws RemoteException {
		System.out.println("Registered " + stationName);
	}
	
	@Override
	public void onSensorUnRegistered(SensorId fullName, Sensor sensor) throws RemoteException {
		System.out.println("Unregistered " + fullName);
	}
	
	@Override
	public void onSensorRegistered(SensorId fullName, Sensor sensor) throws RemoteException {
		System.out.println("Registered " + fullName);
	}
}