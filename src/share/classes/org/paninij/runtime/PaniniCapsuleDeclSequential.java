/**
 * 
 */
package org.paninij.runtime;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation applied to classes that represents that 
 * this class was generated during compilation of a capsule.
 * 
 * @author Hridesh Rajan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited()
public @interface PaniniCapsuleDeclSequential {
	String params();
	boolean definedRun();
}

