package implementations;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;

import sensor.Sensor;
import sensor.SensorParameter;
import sensor.SensorState;
import sensor.SensorState.State;
import utils.Utils;

public abstract class SensorServer extends UnicastRemoteObject implements Sensor {
	private static final long serialVersionUID = 8455786461927369862L;
	protected final SensorState state;

	protected SensorServer() throws RemoteException {
		super();
		this.state = new SensorState(State.SETUP, "Set up");	
	}

	/**
	 * Use this method to perform the initialization of the sensor. Throw any
	 * exception that can not be handled internally to allow Sensor Stations to
	 * abort the registration of the sensor
	 */
	public abstract void setUp() throws Exception;

	public synchronized final SensorState getState() throws RemoteException {
		return state;
	}

	public abstract void tearDown();

	public final void loadParametersFromFile(File propertyFile) {
		if (propertyFile != null) {
			Properties properties = new Properties();
			try {
				InputStream inputStream = new FileInputStream(propertyFile);
				properties.load(inputStream);
				inputStream.close();
			} catch (IOException e) {
				System.out.println("Properties loading from " + propertyFile + " failed");
				return;
			}
			loadParameters(properties);
		}
	}

	public final void loadParametersFromXML(File propertyFile) {
		if (propertyFile != null) {
			Properties properties = new Properties();
			try {
				InputStream inputStream = new FileInputStream(propertyFile);
				properties.loadFromXML(inputStream);
				inputStream.close();
			} catch (IOException e) {
				System.out.println("Properties loading  from " + propertyFile + " failed");
				return;
			}
			loadParameters(properties);
		}
	}

	public final void loadParameters(Properties properties) {
		for (Field f : getAllSensorParameterFields()) {
			try {
			if (f.isAnnotationPresent(SensorParameter.class)) {
				String propertyName = f.getAnnotation(SensorParameter.class).propertyName();
				if (properties.containsKey(propertyName)) {
					try {
						Class<?> typeToParse = f.getType();
						f.set(this,
								typeToParse.getMethod("valueOf", String.class).invoke(null, properties.get(propertyName)));
					} catch (InvocationTargetException ignore) {
						// probabilemte un problema di parsing dei numeri
						System.out.println("Exception: " + ignore.getTargetException().getMessage());
					} catch (NoSuchMethodException e) {
						// non dovrebbe mai avvenire perchè tutti i
						// campi di SensorParameter.validTypes
						// hanno il metodo valueOf(String)
					}
				}
			}
			} catch (IllegalAccessException e) {
				// Questa implementazione del sensore dichiara dei parametri non pubblici
				e.printStackTrace();
			}
		}
	}

	/**
	 * Checks if all fields (declared by the class itself or inherited) that are
	 * annotated with {@link SensorParameter} contain a value
	 * 
	 * @return true if they are, false if one of them is null or if an
	 *         {@link IllegalAccessException} occurs
	 */
	protected final boolean allParametersFilledUp() {
		try {
			for (Field f : getAllSensorParameterFields())
				if (f.isAnnotationPresent(SensorParameter.class) && f.get(this) == null)
					return false;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public final Field[] getAllSensorParameterFields() {
		return Utils.concatenate(this.getClass().getFields(), this.getClass().getDeclaredFields());
	}
}
