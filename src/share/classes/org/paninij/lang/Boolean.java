/*
 * Copyright (c) 1994, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.paninij.lang;

import org.paninij.runtime.types.Panini$Duck;
import java.lang.String;
/**
 * The {@code Boolean} class wraps a value of the primitive type {@code
 * boolean} in an object. An object of type {@code Boolean} contains a
 * single field value whose type is {@code boolean}. Adapted from
 * {@link java.lang.Boolean} to act like a Long and provide the
 * 'duck' behavior required by capsules.
 */
public class Boolean extends Object implements java.io.Serializable,
    Panini$Duck<java.lang.Boolean>, Comparable<Boolean> {
	
	/**
     * The {@code Boolean} object corresponding to the primitive
     * value {@code true}.
     */
    public static final java.lang.Boolean TRUE = java.lang.Boolean.TRUE; //TODO

    /**
     * The {@code Boolean} object corresponding to the primitive
     * value {@code false}.
     */
    public static final java.lang.Boolean FALSE = java.lang.Boolean.FALSE; //TODO
	
	/**
     * The Class object representing the primitive type boolean.
     *
     * @since   JDK1.1
     */
    public static final Class<java.lang.Boolean> TYPE = java.lang.Boolean.TYPE;
    
    /**
     * Returns a {@code String} object representing the specified
     * boolean.  If the specified boolean is {@code true}, then
     * the string {@code "true"} will be returned, otherwise the
     * string {@code "false"} will be returned.
     *
     * @param b the boolean to be converted
     * @return the string representation of the specified {@code boolean}
     * @since 1.4
     */
    public static String toString(boolean b) {
        return java.lang.Boolean.toString(b);
    }
    
    /**
     * Parses the string argument as a boolean.  The {@code boolean}
     * returned represents the value {@code true} if the string argument
     * is not {@code null} and is equal, ignoring case, to the string
     * {@code "true"}. <p>
     * Example: {@code Boolean.parseBoolean("True")} returns {@code true}.<br>
     * Example: {@code Boolean.parseBoolean("yes")} returns {@code false}.
     *
     * @param      s   the {@code String} containing the boolean
     *                 representation to be parsed
     * @return     the boolean represented by the string argument
     * @since 1.5
     */
    public static boolean parseBoolean(String s) {
        return java.lang.Boolean.parseBoolean(s);
    }
    
    /**
     * Returns a {@code Boolean} with a value represented by the
     * specified string.  The {@code Boolean} returned represents a
     * true value if the string argument is not {@code null}
     * and is equal, ignoring case, to the string {@code "true"}.
     *
     * @param   s   a string.
     * @return  the {@code Boolean} value represented by the string.
     */
    public static Boolean valueOf(String s) {
        return new Boolean(parseBoolean(s));
    }
    
    /**
     * Returns a {@code Boolean} instance representing the specified
     * {@code boolean} value.  If the specified {@code boolean} value
     * is {@code true}, this method returns {@code Boolean.TRUE};
     * if it is {@code false}, this method returns {@code Boolean.FALSE}.
     * If a new {@code Boolean} instance is not required, this method
     * should generally be used in preference to the constructor
     * {@link #Boolean(boolean)}, as this method is likely to yield
     * significantly better space and time performance.
     *
     * @param  b a boolean value.
     * 
     * @return a {@code Boolean} instance representing {@code b}.
     * @since  1.4
     */
    public static Boolean valueOf(boolean b) {
        return new Boolean(b);
    }

    /**
     * The value of the {@code Boolean}.
     *
     * This value is filled when either:
     * <ol>
     * <li> an {@code org.paninij.lang.Boolean} is constructed with the actual value</li>
     * <li> the {@link #panini$finish(Boolean)} method is called. </li>
     * </ol>
     * @serial
     */
    private boolean value;

    // Panini$Duck management
    // Panini$Duck management adapted from the output of panc 0.9.1
    // with the -XD-printflat flag for an Integer wrapper type of class.
    /**
     * Message ID for the generated dispatcher.
     */
    private final int panini$message$id;
    /**
     * Boolean indicating whether or not the actual value has been set.
     * Should only be set in either a constructor that has an actual value
     * or when the {@link #panini$finish(Boolean)} method is called.
     */
    private boolean panini$redeemed;

    /**
     * Save the value and notify listeners the value is available.
     *
     * @param l    {@code java.lang.Boolean} to use as a value. Method will pull
     *             the wrapped {@code boolean} out as the value.
     */
    @Override
    public void panini$finish(java.lang.Boolean b) {
        synchronized(this) {
            value = b.booleanValue();
            panini$redeemed = true;
            notifyAll();
        }
    }

    /**
     * Save the value and notify listeners the value is available.
     *
     * Does the same thing as {@link #panini$finish(Boolean)}, but for
     * a {@code boolean} instead of a {@code Boolean}.
     *
     * @param b    {@code boolean} to use as a value.
     */
    public void panini$finish(boolean b) {
        synchronized(this) {
            value = b;
            panini$redeemed = true;
            notifyAll();
        }
    }

    /**
     * Get the message id for the duck. The message id is used by the generated
     * dispatchers to choose what method is supposed to run to fill in the value
     * of the duck.
     */
    @Override
    public int panini$message$id() {
        return this.panini$message$id;
    }

    /**
     * Get the {@link java.lang.Boolean} the
     * duck wraps. Getting the value will force
     * a wait until the actual value is set.
     */
    @Override
    public java.lang.Boolean panini$get() {
        while (panini$redeemed == false) {
            try{
                synchronized (this) {
                    while (panini$redeemed == false) {
                        wait();
                    }
                }
            }catch (InterruptedException e){
            }
        }
        return java.lang.Boolean.valueOf(value);
    }
    // End Panini$Duck management

    /**
     * Constructs a new {@code Duck$Boolean} which does
     * not yet have is value set.
     *
     * @param panini$message$id    message id (method to call) when this
     *        duck is serviced in the message queue.
     */
    public Boolean(int panini$message$id) {
        this.panini$message$id = panini$message$id;
        this.panini$redeemed = false;
    }

    /**
     * Constructs a newly allocated {@code Boolean} object that
     * represents the specified {@code boolean} argument.
     *
     * A {@code Duck$Boolean} constructed with this constructor
     * is available immediately.
     *
     * @param   value   the value to be represented by the
     *          {@code Boolean} object.
     * 
     */
    public Boolean(boolean value) {
        this.value = value;
        this.panini$message$id = 0;
        this.panini$redeemed = true;
    }

    /**
     * Constructs a newly allocated {@code Boolean} object that
     * represents the {@code boolean} value indicated by the
     * {@code String} parameter. 
     *
     * A {@code Duck$Boolean} constructed with this constructor
     * is available immediately.
     *
     * @param      s   the {@code String} to be converted to a
     *             {@code Boolean}.
     * 
     * @throws     NumberFormatException  if the {@code String} does not
     *             contain a parsable {@code boolean}.
     * @see        java.lang.Boolean#parseBoolean(java.lang.String, int)
     */
    public Boolean(String s) throws NumberFormatException {
        this.value = parseBoolean(s);
        this.panini$message$id = 0;
        this.panini$redeemed = true;
    }

    /**
     * Returns the value of this {@code Boolean} object as a boolean
     * primitive.
     *
     * @return  the primitive {@code boolean} value of this object.
     */
    public boolean booleanValue() {
    	if (panini$redeemed == false) panini$get();
        return value;
    }
    
    /**
     * Returns a {@code String} object representing this Boolean's
     * value.  If this object represents the value {@code true},
     * a string equal to {@code "true"} is returned. Otherwise, a
     * string equal to {@code "false"} is returned.
     *
     * @return  a string representation of this object.
     */
    public String toString() {
    	if (panini$redeemed == false) panini$get();
        return toString(value);
    }
    
    /**
     * Returns a hash code for this {@code Boolean} object.
     *
     * @return  the integer {@code 1231} if this object represents
     * {@code true}; returns the integer {@code 1237} if this
     * object represents {@code false}.
     */
    public int hashCode() {
    	if (panini$redeemed == false) panini$get();
        return value ? 1231 : 1237;
    }
    
    /**
     * Returns {@code true} if and only if the argument is not
     * {@code null} and is a {@code Boolean} object that
     * represents the same {@code boolean} value as this object.
     *
     * @param   obj   the object to compare with.
     * @return  {@code true} if the Boolean objects represent the
     *          same value; {@code false} otherwise.
     */
    public boolean equals(java.lang.Object obj) {
    	if (panini$redeemed == false) panini$get();
        if (obj instanceof Boolean) {
        	Boolean other = (Boolean)obj;
            return value == other.booleanValue();
        }
        else if(obj instanceof java.lang.Boolean){
        	java.lang.Boolean other = (java.lang.Boolean)obj;
        	return value == other.booleanValue();
        }
        return false;
    }

    /**
     * Returns {@code true} if and only if the system property
     * named by the argument exists and is equal to the string
     * {@code "true"}. (Beginning with version 1.0.2 of the
     * Java<small><sup>TM</sup></small> platform, the test of
     * this string is case insensitive.) A system property is accessible
     * through {@code getProperty}, a method defined by the
     * {@code System} class.
     * <p>
     * If there is no property with the specified name, or if the specified
     * name is empty or null, then {@code false} is returned.
     *
     * @param   name   the system property name.
     * @return  the {@code boolean} value of the system property.
     * @see     java.lang.System#getProperty(java.lang.String)
     * @see     java.lang.System#getProperty(java.lang.String, java.lang.String)
     */
    public static boolean getBoolean(String name) {
        return java.lang.Boolean.getBoolean(name);
    }
    
    /**
     * Compares this {@code Boolean} instance with another.
     *
     * @param   b the {@code Boolean} instance to be compared
     * @return  zero if this object represents the same boolean value as the
     *          argument; a positive value if this object represents true
     *          and the argument represents false; and a negative value if
     *          this object represents false and the argument represents true
     * @throws  NullPointerException if the argument is {@code null}
     * @see     Comparable
     * @since  1.5
     */
    public int compareTo(Boolean anotherBoolean) {
    	if (panini$redeemed == false) panini$get();
        return compare(this.value, anotherBoolean.booleanValue());
    }
    
    /**
     * Compares this {@code Boolean} instance with another.
     *
     * @param   b the {@code Boolean} instance to be compared
     * @return  zero if this object represents the same boolean value as the
     *          argument; a positive value if this object represents true
     *          and the argument represents false; and a negative value if
     *          this object represents false and the argument represents true
     * @throws  NullPointerException if the argument is {@code null}
     * @see     Comparable
     * @since  1.5
     */
    public int compareTo(java.lang.Boolean anotherBoolean) {
    	if (panini$redeemed == false) panini$get();
        return compare(this.value, anotherBoolean.booleanValue());
    }
    
    /**
     * Compares two {@code boolean} values.
     * The value returned is identical to what would be returned by:
     * <pre>
     *    Boolean.valueOf(x).compareTo(Boolean.valueOf(y))
     * </pre>
     *
     * @param  x the first {@code boolean} to compare
     * @param  y the second {@code boolean} to compare
     * @return the value {@code 0} if {@code x == y};
     *         a value less than {@code 0} if {@code !x && y}; and
     *         a value greater than {@code 0} if {@code x && !y}
     * @since 1.7
     */
    public static int compare(boolean x, boolean y) {
        return (x == y) ? 0 : (x ? 1 : -1);
    }

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = -3665804199014368530L;
}
