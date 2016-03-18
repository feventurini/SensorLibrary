package implementations;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import sensor.Sensor;
import sensor.SensorParameter;
import sensor.SensorState;
import sensor.SensorState.State;

public abstract class SensorServer extends UnicastRemoteObject implements Sensor {
	private static final long serialVersionUID = 8455786461927369862L;
	protected final SensorState state;

	protected SensorServer() throws RemoteException {
		super();
		state = new SensorState(State.SETUP, "Set up");
		for (Field f : getAllSensorParameterFields())
			if (!isValidType(f.getType()))
				throw new RemoteException("", new IllegalClassFormatException(
						f.getName() + " is not a valid parameter field for the sensor"));
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
				if (f.get(this) == null)
					return false;
		} catch (IllegalAccessException ignore) {
			// already checked if public
			ignore.printStackTrace();
		}
		return true;
	}

	private boolean isValidType(Class<?> klass) {
		if (klass == String.class)
			return true;
		else {
			try {
				klass.getMethod("valueOf", String.class);
			} catch (NoSuchMethodException e) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return all the public fields annotated with {@link SensorParameter} of
	 *         this sensor (declared or inherited)
	 */
	public final Set<Field> getAllSensorParameterFields() {
		Set<Field> result = new HashSet<>();
		for (Field f : this.getClass().getFields())
			if (f.getModifiers() == Modifier.PUBLIC && f.isAnnotationPresent(SensorParameter.class)) {
				result.add(f);
			}
		for (Field f : this.getClass().getDeclaredFields())
			if (f.getModifiers() == Modifier.PUBLIC && f.isAnnotationPresent(SensorParameter.class)) {
				result.add(f);
			}
		return result;
	}

	@Override
	public synchronized final SensorState getState() throws RemoteException {
		return state;
	}

	public final void loadParameters(Properties properties) {
		for (Field f : getAllSensorParameterFields()) {
			try {
				String propertyName = f.getAnnotation(SensorParameter.class).propertyName();
				if (properties.containsKey(propertyName)) {
					try {
						Class<?> typeToParse = f.getType();
						f.set(this, typeToParse.getMethod("valueOf", String.class).invoke(null,
								properties.get(propertyName)));
					} catch (InvocationTargetException ignore) {
						// probabilemte un problema di parsing dei numeri
						System.out.println("Exception: " + ignore.getTargetException().getMessage());
					} catch (NoSuchMethodException e) {
						// non dovrebbe mai avvenire perchè tutti i
						// campi di SensorParameter.validTypes
						// hanno il metodo valueOf(String)
					}
				}
			} catch (IllegalAccessException ignore) {
				// already checked if the fields are public
				ignore.printStackTrace();
			}
		}
	}

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

	/**
	 * Use this method to perform the initialization of the sensor. Throw any
	 * exception that can not be handled internally to allow Sensor Stations to
	 * abort the registration of the sensor
	 */
	public abstract void setUp() throws Exception;

	public synchronized void tearDown() {
		state.setState(State.SHUTDOWN);
	}
}
