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
 * The {@code Short} class wraps a value of the primitive type {@code
 * short} in an object. An object of type {@code Short} contains a
 * single field value whose type is {@code short}. Adapted from
 * {@link java.lang.Short} to act like a Long and provide the
 * 'duck' behavior required by capsules.
 */
public class Short extends Number
    implements Panini$Duck<java.lang.Short>, Comparable<Short> {
	/**
     * A constant holding the minimum value a {@code short} can
     * have, -2<sup>15</sup>.
     */
    public static final short   MIN_VALUE = -32768;

    /**
     * A constant holding the maximum value a {@code short} can
     * have, 2<sup>15</sup>-1.
     */
    public static final short   MAX_VALUE = 32767;

    /**
     * The {@code Class} instance representing the primitive type
     * {@code short}.
     *
     * @since   JDK1.1
     */
    public static final Class<java.lang.Short> TYPE = java.lang.Short.TYPE;

    /**
     * Returns a new {@code String} object representing the
     * specified {@code short}. The radix is assumed to be 10.
     *
     * @param s the {@code short} to be converted
     * @return the string representation of the specified {@code short}
     * @see java.lang.Integer#toString(int)
     */
    public static String toString(short s) {
    	return java.lang.Short.toString(s);
    }

    /**
     * Parses the string argument as a signed {@code short} in the
     * radix specified by the second argument. The characters in the
     * string must all be digits, of the specified radix (as
     * determined by whether {@link java.lang.Character#digit(char,
     * int)} returns a nonnegative value) except that the first
     * character may be an ASCII minus sign {@code '-'}
     * (<code>'&#92;u002D'</code>) to indicate a negative value or an
     * ASCII plus sign {@code '+'} (<code>'&#92;u002B'</code>) to
     * indicate a positive value.  The resulting {@code short} value
     * is returned.
     *
     * <p>An exception of type {@code NumberFormatException} is
     * thrown if any of the following situations occurs:
     * <ul>
     * <li> The first argument is {@code null} or is a string of
     * length zero.
     *
     * <li> The radix is either smaller than {@link
     * java.lang.Character#MIN_RADIX} or larger than {@link
     * java.lang.Character#MAX_RADIX}.
     *
     * <li> Any character of the string is not a digit of the
     * specified radix, except that the first character may be a minus
     * sign {@code '-'} (<code>'&#92;u002D'</code>) or plus sign
      * {@code '+'} (<code>'&#92;u002B'</code>) provided that the
      * string is longer than length 1.
      *
      * <li> The value represented by the string is not a value of type
      * {@code short}.
      * </ul>
      *
      * @param s         the {@code String} containing the
      *                  {@code short} representation to be parsed
      * @param radix     the radix to be used while parsing {@code s}
      * @return          the {@code short} represented by the string
      *                  argument in the specified radix.
      * @throws          NumberFormatException If the {@code String}
      *                  does not contain a parsable {@code short}.
      */
     public static short parseShort(String s, int radix)
         throws NumberFormatException {
    	 return java.lang.Short.parseShort(s, radix);
     }

     /**
      * Parses the string argument as a signed decimal {@code
      * short}. The characters in the string must all be decimal
      * digits, except that the first character may be an ASCII minus
      * sign {@code '-'} (<code>'&#92;u002D'</code>) to indicate a
      * negative value or an ASCII plus sign {@code '+'}
      * (<code>'&#92;u002B'</code>) to indicate a positive value.  The
      * resulting {@code short} value is returned, exactly as if the
      * argument and the radix 10 were given as arguments to the {@link
      * #parseShort(java.lang.String, int)} method.
      *
      * @param s a {@code String} containing the {@code short}
      *          representation to be parsed
      * @return  the {@code short} value represented by the
      *          argument in decimal.
      * @throws  NumberFormatException If the string does not
      *          contain a parsable {@code short}.
      */
     public static short parseShort(String s) throws NumberFormatException {
    	 return java.lang.Short.parseShort(s);
     }

     /**
      * Returns a {@code Short} object holding the value
      * extracted from the specified {@code String} when parsed
      * with the radix given by the second argument. The first argument
      * is interpreted as representing a signed {@code short} in
      * the radix specified by the second argument, exactly as if the
      * argument were given to the {@link #parseShort(java.lang.String,
      * int)} method. The result is a {@code Short} object that
      * represents the {@code short} value specified by the string.
      *
      * <p>In other words, this method returns a {@code Short} object
      * equal to the value of:
      *
      * <blockquote>
      *  {@code new Short(Short.parseShort(s, radix))}
      * </blockquote>
      *
      * @param s         the string to be parsed
      * @param radix     the radix to be used in interpreting {@code s}
      * @return          a {@code Short} object holding the value
      *                  represented by the string argument in the
      *                  specified radix.
      * @throws          NumberFormatException If the {@code String} does
      *                  not contain a parsable {@code short}.
      */
     public static Short valueOf(String s, int radix)
         throws NumberFormatException {
    	 return new Short(parseShort(s, radix));
     }

     /**
      * Returns a {@code Short} object holding the
      * value given by the specified {@code String}. The argument
      * is interpreted as representing a signed decimal
      * {@code short}, exactly as if the argument were given to
      * the {@link #parseShort(java.lang.String)} method. The result is
      * a {@code Short} object that represents the
      * {@code short} value specified by the string.
      *
      * <p>In other words, this method returns a {@code Short} object
      * equal to the value of:
      *
      * <blockquote>
      *  {@code new Short(Short.parseShort(s))}
      * </blockquote>
      *
      * @param s the string to be parsed
      * @return  a {@code Short} object holding the value
      *          represented by the string argument
      * @throws  NumberFormatException If the {@code String} does
      *          not contain a parsable {@code short}.
      */
     public static Short valueOf(String s) throws NumberFormatException {
    	 return new Short(parseShort(s));
     }

     /**
      * Returns a {@code Short} instance representing the specified
      * {@code short} value.
      * If a new {@code Short} instance is not required, this method
      * should generally be used in preference to the constructor
      * {@link #Short(short)}, as this method is likely to yield
      * significantly better space and time performance by caching
      * frequently requested values.
      *
      * This method will always cache values in the range -128 to 127,
      * inclusive, and may cache other values outside of this range.
      *
      * @param  s a short value.
      * @return a {@code Short} instance representing {@code s}.
      * @since  1.5
      */
     public static Short valueOf(short s) {
    	 return new Short(s);
     }

     /**
      * Decodes a {@code String} into a {@code Short}.
      * Accepts decimal, hexadecimal, and octal numbers given by
      * the following grammar:
      *
      * <blockquote>
      * <dl>
      * <dt><i>DecodableString:</i>
      * <dd><i>Sign<sub>opt</sub> DecimalNumeral</i>
      * <dd><i>Sign<sub>opt</sub></i> {@code 0x} <i>HexDigits</i>
      * <dd><i>Sign<sub>opt</sub></i> {@code 0X} <i>HexDigits</i>
      * <dd><i>Sign<sub>opt</sub></i> {@code #} <i>HexDigits</i>
      * <dd><i>Sign<sub>opt</sub></i> {@code 0} <i>OctalDigits</i>
      * <p>
      * <dt><i>Sign:</i>
      * <dd>{@code -}
      * <dd>{@code +}
      * </dl>
      * </blockquote>
      *
      * <i>DecimalNumeral</i>, <i>HexDigits</i>, and <i>OctalDigits</i>
      * are as defined in section 3.10.1 of
      * <cite>The Java&trade; Language Specification</cite>,
      * except that underscores are not accepted between digits.
      *
      * <p>The sequence of characters following an optional
      * sign and/or radix specifier ("{@code 0x}", "{@code 0X}",
      * "{@code #}", or leading zero) is parsed as by the {@code
      * Short.parseShort} method with the indicated radix (10, 16, or
      * 8).  This sequence of characters must represent a positive
      * value or a {@link NumberFormatException} will be thrown.  The
      * result is negated if first character of the specified {@code
      * String} is the minus sign.  No whitespace characters are
      * permitted in the {@code String}.
      *
      * @param     nm the {@code String} to decode.
      * @return    a {@code Short} object holding the {@code short}
      *            value represented by {@code nm}
      * @throws    NumberFormatException  if the {@code String} does not
      *            contain a parsable {@code short}.
      * @see java.lang.Short#parseShort(java.lang.String, int)
      */
     public static java.lang.Short decode(String nm) throws NumberFormatException {
    	 return java.lang.Short.decode(nm);
     }

    /**
     * The value of the {@code Short}.
     *
     * This value is filled when either:
     * <ol>
     * <li> an {@code org.paninij.lang.Short} is constructed with the actual value</li>
     * <li> the {@link #panini$finish(Short)} method is called. </li>
     * </ol>
     * @serial
     */
    private short value;

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
     * or when the {@link #panini$finish(Short)} method is called.
     */
    private boolean panini$redeemed;

    /**
     * Save the value and notify listeners the value is available.
     *
     * @param s    {@code java.lang.Short} to use as a value. Method will pull
     *             the wrapped {@code short} out as the value.
     */
    @Override
    public void panini$finish(java.lang.Short s) {
        synchronized(this) {
            value = s.shortValue();
            panini$redeemed = true;
            notifyAll();
        }
    }

    /**
     * Save the value and notify listeners the value is available.
     *
     * Does the same thing as {@link #panini$finish(Short)}, but for
     * a {@code short} instead of a {@code Short}.
     *
     * @param s    {@code short} to use as a value.
     */
    public void panini$finish(short s) {
        synchronized(this) {
            value = s;
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
     * Get the {@link java.lang.Long} the
     * duck wraps. Getting the value will force
     * a wait until the actual value is set.
     */
    @Override
    public java.lang.Short panini$get() {
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
        return java.lang.Short.valueOf(value);
    }
    // End Panini$Duck management

    /**
     * Constructs a new {@code Duck$Short} which does
     * not yet have is value set.
     *
     * @param panini$message$id    message id (method to call) when this
     *        duck is serviced in the message queue.
     */
    public Short(int panini$message$id) {
        this.panini$message$id = panini$message$id;
        this.panini$redeemed = false;
    }

    /**
     * Constructs a newly allocated {@code Short} object that
     * represents the specified {@code short} argument.
     *
     * A {@code Duck$Short} constructed with this constructor
     * is available immediately.
     *
     * @param   value   the value to be represented by the
     *          {@code Short} object.
     * 
     */
    public Short(short value) {
        this.value = value;
        this.panini$message$id = 0;
        this.panini$redeemed = true;
    }

    /**
     * Constructs a newly allocated {@code Short} object that
     * represents the {@code short} value indicated by the
     * {@code String} parameter. The string is converted to a
     * {@code short} value in exactly the manner used by the
     * {@code parseShort} method for radix 10.
     *
     * A {@code Duck$Long} constructed with this constructor
     * is available immediately.
     *
     * @param      s   the {@code String} to be converted to a
     *             {@code Long}.
     * @throws     NumberFormatException  if the {@code String} does not
     *             contain a parsable {@code long}.
     * @see        java.lang.Long#parseLong(java.lang.String, int)
     */
    public Short(String s) throws NumberFormatException {
        this.value = parseShort(s, 10);
        this.panini$message$id = 0;
        this.panini$redeemed = true;
    }

    /**
     * Returns the value of this {@code Short} as a
     * {@code byte}.
     */
    public byte byteValue() {
        if (panini$redeemed == false) panini$get();
        return (byte)value;
    }

    /**
     * Returns the value of this {@code Short} as a
     * {@code short}.
     */
    public short shortValue() {
        if (panini$redeemed == false) panini$get();
        return value;
    }

    /**
     * Returns the value of this {@code Short} as an
     * {@code int}.
     */
    public int intValue() {
        if (panini$redeemed == false) panini$get();
        return (int)value;
    }

    /**
     * Returns the value of this {@code Short} as a
     * {@code long} value.
     */
    public long longValue() {
        if (panini$redeemed == false) panini$get();
        return (long)value;
    }

    /**
     * Returns the value of this {@code Short} as a
     * {@code float}.
     */
    public float floatValue() {
        if (panini$redeemed == false) panini$get();
        return (float)value;
    }

    /**
     * Returns the value of this {@code Short} as a
     * {@code double}.
     */
    public double doubleValue() {
        if (panini$redeemed == false) panini$get();
        return (double)value;
    }

    /**
     * Returns a {@code String} object representing this
     * {@code Short}'s value.  The value is converted to signed
     * decimal representation and returned as a string, exactly as if
     * the {@code short} value were given as an argument to the
     * {@link java.lang.Short#toString(short)} method.
     *
     * @return  a string representation of the value of this object in
     *          base&nbsp;10.
     */
    public String toString() {
        if (panini$redeemed == false) panini$get();
        return toString(value);
    }

    /**
     * Returns a hash code for this {@code Short}; equal to the result
     * of invoking {@code intValue()}.
     *
     * @return a hash code value for this {@code Short}
     */
    public int hashCode() {
    	if (panini$redeemed == false) panini$get();
        return (int)value;
    }

    /**
     * Compares this object to the specified object.  The result is
     * {@code true} if and only if the argument is not
     * {@code null} and is a {@code Short} object that
     * contains the same {@code short} value as this object.
     *
     * @param obj       the object to compare with
     * @return          {@code true} if the objects are the same;
     *                  {@code false} otherwise.
     */
    public boolean equals(java.lang.Object obj) {
    	if (panini$redeemed == false) panini$get();
        if (obj instanceof Short) {
            return value == ((Short)obj).shortValue();
        }
        else if (obj instanceof java.lang.Short){
        	return value == ((java.lang.Short)obj).shortValue();
        }
        return false;
    }

    /**
     * Compares two {@code Short} objects numerically.
     *
     * @param   anotherShort   the {@code Short} to be compared.
     * @return  the value {@code 0} if this {@code Short} is
     *          equal to the argument {@code Short}; a value less than
     *          {@code 0} if this {@code Short} is numerically less
     *          than the argument {@code Short}; and a value greater than
     *           {@code 0} if this {@code Short} is numerically
     *           greater than the argument {@code Short} (signed
     *           comparison).
     * @since   1.2
     */
    public int compareTo(Short anotherShort) {
    	if (panini$redeemed == false) panini$get();
        return compare(this.value, anotherShort.shortValue());
    }
    
    /**
     * Compares two {@code Short} objects numerically.
     *
     * @param   anotherShort   the {@code Short} to be compared.
     * @return  the value {@code 0} if this {@code Short} is
     *          equal to the argument {@code Short}; a value less than
     *          {@code 0} if this {@code Short} is numerically less
     *          than the argument {@code Short}; and a value greater than
     *           {@code 0} if this {@code Short} is numerically
     *           greater than the argument {@code Short} (signed
     *           comparison).
     * @since   1.2
     */
    public int compareTo(java.lang.Short anotherShort) {
    	if (panini$redeemed == false) panini$get();
        return compare(this.value, anotherShort.shortValue());
    }

    /**
     * Compares two {@code short} values numerically.
     * The value returned is identical to what would be returned by:
     * <pre>
     *    Short.valueOf(x).compareTo(Short.valueOf(y))
     * </pre>
     *
     * @param  x the first {@code short} to compare
     * @param  y the second {@code short} to compare
     * @return the value {@code 0} if {@code x == y};
     *         a value less than {@code 0} if {@code x < y}; and
     *         a value greater than {@code 0} if {@code x > y}
     * @since 1.7
     */
    public static int compare(short x, short y) {
        return x - y;
    }

    /**
     * The number of bits used to represent a {@code short} value in two's
     * complement binary form.
     * @since 1.5
     */
    public static final int SIZE = 16;

    /**
     * Returns the value obtained by reversing the order of the bytes in the
     * two's complement representation of the specified {@code short} value.
     *
     * @return the value obtained by reversing (or, equivalently, swapping)
     *     the bytes in the specified {@code short} value.
     * @since 1.5
     */
    public static short reverseBytes(short i) {
        return (short) (((i & 0xFF00) >> 8) | (i << 8));
    }

    /** use serialVersionUID from JDK 1.1. for interoperability */
    private static final long serialVersionUID = 7515723908773894738L;
}
