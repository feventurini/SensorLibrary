package sensor.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SensorServer extends UnicastRemoteObject implements Sensor {
	private static final long serialVersionUID = 8455786461927369862L;

	private final static Logger log = Logger.getLogger(SensorServer.class.getName());

	private SensorState state;

	private List<StateListener> listeners;

	protected SensorServer() throws RemoteException {
		super();
		state = SensorState.SHUTDOWN;
		listeners = new LinkedList<>();
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
						if (typeToParse == String.class) {
							f.set(this, properties.getProperty(propertyName));
						} else {
							f.set(this, typeToParse.getMethod("valueOf", String.class).invoke(null,
									properties.get(propertyName)));
						}
					} catch (InvocationTargetException e) {
						// probabilemte un problema di parsing dei numeri
						log.log(Level.SEVERE,
								getClass().getSimpleName() + ": error while loading the parameter " + propertyName, e);
					} catch (NoSuchMethodException ignore) {
						// non dovrebbe mai avvenire perchè tutti i
						// campi di SensorParameter.validTypes
						// hanno il metodo valueOf(String)
						// a parte String che si tratta a parte
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
				log.log(Level.SEVERE,
						getClass().getSimpleName() + ": properties loading from " + propertyFile + " failed", e);
				return;
			}
			loadParameters(properties);
		}
	}

	/**
	 * This method will check if all the parameters are filled up, then will
	 * call {@link #customSetUp()} to allow the specific implementation to
	 * perform its initialization. If it succeeds the state is set to RUNNING
	 * 
	 * @throws Exception
	 */
	public void setUp() throws Exception {
		if (!allParametersFilledUp())
			throw new IllegalStateException("Missing parameters: " + getAllSensorParameterFields().stream()
					.filter((f) -> f == null).map(Field::getName).collect(Collectors.joining(", ")));
		customSetUp();
		setState(SensorState.RUNNING);
	}

	/**
	 * Use this method to perform the initialization of the sensor. Throw any
	 * exception that can not be handled internally to allow Sensor Stations to
	 * abort the initialization of the sensor
	 */
	protected abstract void customSetUp() throws Exception;

	protected abstract void customTearDown();

	public void tearDown() throws Exception {
		customTearDown();
		setState(SensorState.SHUTDOWN);
	}
	
	protected abstract void customFail();

	public void fail(){
		customFail();
		setState(SensorState.FAULT);
	}


	private final synchronized void setState(SensorState state) {
		SensorState old = this.state;
		this.state = state;

		switch (state) {
		case RUNNING:
			log.info(this.getClass().getSimpleName() + " started");
			break;
		case SHUTDOWN:
			log.info(this.getClass().getSimpleName() + " stopped");
			break;
		case FAULT:
			log.info(this.getClass().getSimpleName() + " state set to fault");
			break;
		}

		if (!listeners.isEmpty()) {
			ExecutorService executorService = Executors.newFixedThreadPool(listeners.size());
			listeners.forEach((l) -> executorService.submit(() -> {
				try {
					l.onStateChange(this, old, state);
				} catch (RemoteException e) {
					log.log(Level.WARNING, getClass().getSimpleName() + ": exception in Listener", e.getCause());
				}
			}));
			// shutdown non ferma i thread già presenti, impedisce di
			// aggiungerne nuovi,
			// finalizza l'executorService quando si è svuotato, non è bloccante
			// per chi lo chiama
			executorService.shutdown();
		}
	}

	@Override
	public synchronized final void addListener(StateListener listener) throws RemoteException {
		listeners.add(listener);
		log.info(getClass().getSimpleName() + ": added listener");
	}

	@Override
	public synchronized final void removeListeners(StateListener listener) {
		listeners.remove(listener);
		log.info(getClass().getSimpleName() + ": removed listener");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sensor.base.Sensor#getInterfaces()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Class<? extends Sensor>> getSensorInterfaces() throws RemoteException {
		return Stream.of(getClass().getInterfaces()).filter(Sensor.class::isAssignableFrom)
				.map((k) -> (Class<? extends Sensor>) k).collect(Collectors.toList());
	}
}
