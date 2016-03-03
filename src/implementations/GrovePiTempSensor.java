package implementations;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.iot.raspberry.grovepi.devices.GroveTemperatureAndHumiditySensor;
import org.iot.raspberry.grovepi.pi4j.GrovePi4J;

import sensor.FutureResult;
import sensor.FutureResultImpl;
import sensor.TempSensor;

public class GrovePiTempSensor extends SensorServer implements TempSensor {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5353227817012312834L;
	private GroveTemperatureAndHumiditySensor sensor;

	public GrovePiTempSensor() throws RemoteException {
		super();
	}

	private double measure() throws IOException {
		return sensor.get().getTemperature();
	}

	@Override
	public synchronized Double readTemperature(Unit unit) throws RemoteException {
		return readTemperatureAsync(unit).get();
	}

	@Override
	public synchronized FutureResult<Double> readTemperatureAsync(Unit unit) throws RemoteException {
		FutureResultImpl<Double> result = new FutureResultImpl<>();
		// TODO eseguire i task async dentro un threadpoolexecutor nostro e non quello comune
		CompletableFuture.supplyAsync(new Supplier<Double>() {
			@Override
			public Double get() {
				try {
					return measure();
				} catch (IOException e) {
					return (double) -1;
				}
			}
		}).thenAccept(new Consumer<Double>() {

			@Override
			public void accept(Double t) {
				result.set(t);
				
			}
		});
		return result;
	}

	@Override
	public void setUp() throws Exception {
		if (!allParametersFilledUp()) {
			isSetUp = false;
		} else {
			try {
				this.sensor = new GroveTemperatureAndHumiditySensor(new GrovePi4J(), 2,
						GroveTemperatureAndHumiditySensor.Type.DHT11);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void tearDown() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getState() {
		// TODO Auto-generated method stub
		return null;
	}

}
