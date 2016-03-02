package sensor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SensorParameter {
	String userDescription();
	// si poteva usare il nome del field come key nelle property, così facendo
	// c'è più complessità, ma anche maggiore flessibilità
	String propertyName();
}
