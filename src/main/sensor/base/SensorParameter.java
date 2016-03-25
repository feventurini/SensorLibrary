package sensor.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Properties;

/**
 * All parameters types must have the static method valueOf(String) so that when
 * initializing the sensor the following code can be used:<br>
 * <code>
 * String value = "1";<br>
 * typeToParse.getMethod("valueOf", String.class).invoke(null, value.trim());<br>
 * </code>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SensorParameter {

	// NB: si poteva usare il nome del field per recuperare il valore nei file
	// di property,
	// si sceglie invece di definire una key ulteriore per tenere disaccoppiati
	// i file di property
	// e i nomi dei parametri, così facendo c'è più complessità, ma anche
	// maggiore flessibilità
	// potendo rinominare i field e le key in maniera indipendente

	/**
	 * The string to be used as key to retrieve the value of the parameter from
	 * {@link Properties}
	 * 
	 * @return
	 */
	String propertyName();

	/**
	 * A user friendly description of what the parameter represents, can be used
	 * during initialization to query the user for a value
	 * 
	 * @return
	 */
	String userDescription();
}
