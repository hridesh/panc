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
 * An annotation applied to interfaces that represents that 
 * this interface was generated during compilation of a capsule.
 * 
 * @author Hridesh Rajan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited()
public @interface PaniniCapsuleDeclInterface {
	String params();
	boolean definedRun();
}

