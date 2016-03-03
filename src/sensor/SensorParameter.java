package sensor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SensorParameter {
	/**
	 * Collection of valid types for this annotation, note that the enforcement of the parameter type must be done externally.<br>
	 * <code>
	 * Field f<br>
	 * Class<?> typeToParse = f.getType()<br>
	 * if (SensorParameter.validTypes.contains(typeToParse))<br>
	 * .....<br>
	 * </code>
	 * All of these types have the method valueOf(String) so you can use.<br>
	 * <code>
	 * typeToParse.getMethod("valueOf", String.class).invoke(value.trim());<br>
	 * </code>
	 */
	Set<Class<?>> validTypes = new HashSet<>(Arrays.asList(String.class, Integer.class, Double.class, Boolean.class, Long.class));

	// NB: si poteva usare il nome del field per recuperare il valore nei file di property,
	// si sceglie invece di definire una key ulteriore per tenere disaccoppiati i file di property
	// e i nomi dei parametri, così facendo c'è più complessità, ma anche maggiore flessibilità
	// potendo rinominare i field e le key in maniera indipendente
	
	String propertyName();
	String userDescription();
}
