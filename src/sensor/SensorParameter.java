package sensor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * All parameters types must have the method valueOf(String) so you can  use.<br>
 * <code>
 * typeToParse.getMethod("valueOf", String.class).invoke(value.trim());<br>
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

	String propertyName();

	String userDescription();
}
