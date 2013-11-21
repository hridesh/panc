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

import org.paninij.runtime.PaniniBatchMessage;
import org.paninij.runtime.types.Panini$Duck;

import java.lang.String;
/**
 * The {@code Byte} class wraps a value of the primitive type {@code
 * byte} in an object. An object of type {@code Byte} contains a
 * single field value whose type is {@code byte}. Adapted from
 * {@link java.lang.Byte} to act like a Long and provide the
 * 'duck' behavior required by capsules.
 */
public class Byte extends Number
    implements Panini$Duck<java.lang.Byte>, Comparable<Byte> {
	/**
     * A constant holding the minimum value a {@code byte} can
     * have, -2<sup>7</sup>.
     */
    public static final byte   MIN_VALUE = -128;

    /**
     * A constant holding the maximum value a {@code byte} can
     * have, 2<sup>7</sup>-1.
     */
    public static final byte   MAX_VALUE = 127;

    /**
     * The {@code Class} instance representing the primitive type
     * {@code byte}.
     *
     * @since   JDK1.1
     */
    public static final Class<java.lang.Byte> TYPE = java.lang.Byte.TYPE;

    /**
     * Returns a new {@code String} object representing the
     * specified {@code byte}. The radix is assumed to be 10.
     *
     * @param b the {@code byte} to be converted
     * @return the string representation of the specified {@code byte}
     * @see java.lang.Integer#toString(int)
     */
    public static String toString(byte b) {
        return java.lang.Byte.toString(b);
    }

    /**
     * Parses the string argument as a signed {@code byte} in the
     * radix specified by the second argument. The characters in the
     * string must all be digits, of the specified radix (as
     * determined by whether {@link java.lang.Character#digit(char,
     * int)} returns a nonnegative value) except that the first
     * character may be an ASCII minus sign {@code '-'}
     * (<code>'&#92;u002D'</code>) to indicate a negative value or an
     * ASCII plus sign {@code '+'} (<code>'&#92;u002B'</code>) to
     * indicate a positive value.  The resulting {@code byte} value is
     * returned.
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
     * {@code byte}.
     * </ul>
     *
     * @param s         the {@code String} containing the
     *                  {@code byte}
     *                  representation to be parsed
     * @param radix     the radix to be used while parsing {@code s}
     * @return          the {@code byte} value represented by the string
     *                   argument in the specified radix
     * @throws          NumberFormatException If the string does
     *                  not contain a parsable {@code byte}.
     */
    public static byte parseByte(String s, int radix)
        throws NumberFormatException {
    	return java.lang.Byte.parseByte(s, radix);
    }

    /**
     * Parses the string argument as a signed decimal {@code
     * byte}. The characters in the string must all be decimal digits,
     * except that the first character may be an ASCII minus sign
     * {@code '-'} (<code>'&#92;u002D'</code>) to indicate a negative
     * value or an ASCII plus sign {@code '+'}
     * (<code>'&#92;u002B'</code>) to indicate a positive value. The
     * resulting {@code byte} value is returned, exactly as if the
     * argument and the radix 10 were given as arguments to the {@link
     * #parseByte(java.lang.String, int)} method.
     *
     * @param s         a {@code String} containing the
     *                  {@code byte} representation to be parsed
     * @return          the {@code byte} value represented by the
     *                  argument in decimal
     * @throws          NumberFormatException if the string does not
     *                  contain a parsable {@code byte}.
     */
    public static byte parseByte(String s) throws NumberFormatException {
    	return java.lang.Byte.parseByte(s);
    }

    /**
     * Returns a {@code Byte} instance representing the specified
     * {@code byte} value.
     * If a new {@code Byte} instance is not required, this method
     * should generally be used in preference to the constructor
     * {@link #Byte(byte)}, as this method is likely to yield
     * significantly better space and time performance since
     * all byte values are cached.
     *
     * @param  b a byte value.
     * @return a {@code Byte} instance representing {@code b}.
     * @since  1.5
     */
     public static Byte valueOf(byte b) {
    	 return new Byte(b);
     }

     /**
      * Returns a {@code Byte} object holding the value
      * extracted from the specified {@code String} when parsed
      * with the radix given by the second argument. The first argument
      * is interpreted as representing a signed {@code byte} in
      * the radix specified by the second argument, exactly as if the
      * argument were given to the {@link #parseByte(java.lang.String,
      * int)} method. The result is a {@code Byte} object that
      * represents the {@code byte} value specified by the string.
      *
      * <p> In other words, this method returns a {@code Byte} object
      * equal to the value of:
      *
      * <blockquote>
      * {@code new Byte(Byte.parseByte(s, radix))}
      * </blockquote>
      *
      * @param s         the string to be parsed
      * @param radix     the radix to be used in interpreting {@code s}
      * @return          a {@code Byte} object holding the value
      *                  represented by the string argument in the
      *                  specified radix.
      * @throws          NumberFormatException If the {@code String} does
      *                  not contain a parsable {@code byte}.
      */
     public static Byte valueOf(String s, int radix)
         throws NumberFormatException {
    	 return new Byte(parseByte(s,radix));
     }

     /**
      * Returns a {@code Byte} object holding the value
      * given by the specified {@code String}. The argument is
      * interpreted as representing a signed decimal {@code byte},
      * exactly as if the argument were given to the {@link
      * #parseByte(java.lang.String)} method. The result is a
      * {@code Byte} object that represents the {@code byte}
      * value specified by the string.
      *
      * <p> In other words, this method returns a {@code Byte} object
      * equal to the value of:
      *
      * <blockquote>
      * {@code new Byte(Byte.parseByte(s))}
      * </blockquote>
      *
      * @param s         the string to be parsed
      * @return          a {@code Byte} object holding the value
      *                  represented by the string argument
      * @throws          NumberFormatException If the {@code String} does
      *                  not contain a parsable {@code byte}.
      */
     public static Byte valueOf(String s) throws NumberFormatException {
    	 return new Byte(parseByte(s));
     }

     /**
      * Decodes a {@code String} into a {@code Byte}.
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
      * Byte.parseByte} method with the indicated radix (10, 16, or 8).
      * This sequence of characters must represent a positive value or
      * a {@link NumberFormatException} will be thrown.  The result is
      * negated if first character of the specified {@code String} is
      * the minus sign.  No whitespace characters are permitted in the
      * {@code String}.
      *
      * @param     nm the {@code String} to decode.
      * @return   a {@code Byte} object holding the {@code byte}
      *          value represented by {@code nm}
      * @throws  NumberFormatException  if the {@code String} does not
      *            contain a parsable {@code byte}.
      * @see java.lang.Byte#parseByte(java.lang.String, int)
      */
     public static java.lang.Byte decode(String nm) throws NumberFormatException {
    	 return java.lang.Byte.decode(nm);
     }

    /**
     * The value of the {@code Byte}.
     *
     * This value is filled when either:
     * <ol>
     * <li> an {@code org.paninij.lang.Byte} is constructed with the actual value</li>
     * <li> the {@link #panini$finish(Byte)} method is called. </li>
     * </ol>
     * @serial
     */
    private byte value;

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
     * or when the {@link #panini$finish(Byte)} method is called.
     */
    private boolean panini$redeemed;

    /**
     * Save the value and notify listeners the value is available.
     *
     * @param b    {@code java.lang.Byte} to use as a value. Method will pull
     *             the wrapped {@code byte} out as the value.
     */
    @Override
    public void panini$finish(java.lang.Byte b) {
        synchronized(this) {
            value = b.byteValue();
            panini$redeemed = true;
            notifyAll();
        }
    }

    /**
     * Save the value and notify listeners the value is available.
     *
     * Does the same thing as {@link #panini$finish(Byte)}, but for
     * a {@code byte} instead of a {@code Byte}.
     *
     * @param b    {@code byte} to use as a value.
     */
    public void panini$finish(byte b) {
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
     * Get the {@link java.lang.Byte} the
     * duck wraps. Getting the value will force
     * a wait until the actual value is set.
     */
    @Override
    public java.lang.Byte panini$get() {
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
        return java.lang.Byte.valueOf(value);
    }
    // End Panini$Duck management

    /**
     * Constructs a new {@code Duck$Byte} which does
     * not yet have is value set.
     *
     * @param panini$message$id    message id (method to call) when this
     *        duck is serviced in the message queue.
     */
    public Byte(int panini$message$id) {
        this.panini$message$id = panini$message$id;
        this.panini$redeemed = false;
    }

    /**
     * Constructs a newly allocated {@code Byte} object that
     * represents the specified {@code long} argument.
     *
     * A {@code Duck$Byte} constructed with this constructor
     * is available immediately.
     *
     * @param   value   the value to be represented by the
     *          {@code Byte} object.
     * 
     */
    public Byte(byte value) {
        this.value = value;
        this.panini$message$id = 0;
        this.panini$redeemed = true;
    }

    /**
     * Constructs a newly allocated {@code Byte} object that
     * represents the {@code byte} value indicated by the
     * {@code String} parameter. The string is converted to a
     * {@code byte} value in exactly the manner used by the
     * {@code parseByte} method for radix 10.
     *
     * A {@code Duck$Long} constructed with this constructor
     * is available immediately.
     *
     * @param      s   the {@code String} to be converted to a
     *             {@code Byte}.
     * @throws     NumberFormatException  if the {@code String} does not
     *             contain a parsable {@code byte}.
     * @see        java.lang.Byte#parseByte(java.lang.String, int)
     */
    public Byte(String s) throws NumberFormatException {
        this.value = parseByte(s, 10);
        this.panini$message$id = 0;
        this.panini$redeemed = true;
    }

    /**
     * Returns the value of this {@code Byte} as a
     * {@code byte}.
     */
    public byte byteValue() {
        if (panini$redeemed == false) panini$get();
        return value;
    }

    /**
     * Returns the value of this {@code Byte} as a
     * {@code short}.
     */
    public short shortValue() {
        if (panini$redeemed == false) panini$get();
        return (short)value;
    }

    /**
     * Returns the value of this {@code Byte} as an
     * {@code int}.
     */
    public int intValue() {
        if (panini$redeemed == false) panini$get();
        return (int)value;
    }

    /**
     * Returns the value of this {@code Byte} as a
     * {@code long} value.
     */
    public long longValue() {
        if (panini$redeemed == false) panini$get();
        return (long)value;
    }

    /**
     * Returns the value of this {@code Byte} as a
     * {@code float}.
     */
    public float floatValue() {
        if (panini$redeemed == false) panini$get();
        return (float)value;
    }

    /**
     * Returns the value of this {@code Byte} as a
     * {@code double}.
     */
    public double doubleValue() {
        if (panini$redeemed == false) panini$get();
        return (double)value;
    }

    /**
     * Returns a {@code String} object representing this
     * {@code Byte}'s value.  The value is converted to signed
     * decimal representation and returned as a string, exactly as if
     * the {@code byte} value were given as an argument to the
     * {@link java.lang.Byte#toString(byte)} method.
     *
     * @return  a string representation of the value of this object in
     *          base&nbsp;10.
     */
    public String toString() {
        if (panini$redeemed == false) panini$get();
        return toString(value);
    }

    /**
     * Returns a hash code for this {@code Byte}; equal to the result
     * of invoking {@code intValue()}.
     *
     * @return a hash code value for this {@code Byte}
     */
    public int hashCode() {
    	if(panini$redeemed == false) panini$get();
        return (int)value;
    }

    /**
     * Compares this object to the specified object.  The result is
     * {@code true} if and only if the argument is not
     * {@code null} and is a {@code Byte} object that
     * contains the same {@code byte} value as this object.
     *
     * @param obj       the object to compare with
     * @return          {@code true} if the objects are the same;
     *                  {@code false} otherwise.
     */
    public boolean equals(java.lang.Object obj) {
    	if (panini$redeemed == false) panini$get();
        if (obj instanceof Byte) {
            return value == ((Byte)obj).byteValue();
        }
        else if (obj instanceof java.lang.Byte){
        	return value == ((java.lang.Byte)obj).byteValue();
        }
        return false;
    }

    /**
     * Compares two {@code Byte} objects numerically.
     *
     * @param   anotherByte   the {@code Byte} to be compared.
     * @return  the value {@code 0} if this {@code Byte} is
     *          equal to the argument {@code Byte}; a value less than
     *          {@code 0} if this {@code Byte} is numerically less
     *          than the argument {@code Byte}; and a value greater than
     *           {@code 0} if this {@code Byte} is numerically
     *           greater than the argument {@code Byte} (signed
     *           comparison).
     * @since   1.2
     */
    public int compareTo(Byte anotherByte) {
    	if (panini$redeemed == false) panini$get();
        return compare(this.value, anotherByte.byteValue());
    }

    /**
     * Compares two {@code Byte} objects numerically.
     *
     * @param   anotherByte   the {@code Byte} to be compared.
     * @return  the value {@code 0} if this {@code Byte} is
     *          equal to the argument {@code Byte}; a value less than
     *          {@code 0} if this {@code Byte} is numerically less
     *          than the argument {@code Byte}; and a value greater than
     *           {@code 0} if this {@code Byte} is numerically
     *           greater than the argument {@code Byte} (signed
     *           comparison).
     * @since   1.2
     */
    public int compareTo(java.lang.Byte anotherByte) {
    	if (panini$redeemed == false) panini$get();
        return compare(this.value, anotherByte.byteValue());
    }

    /**
     * Compares two {@code byte} values numerically.
     * The value returned is identical to what would be returned by:
     * <pre>
     *    Byte.valueOf(x).compareTo(Byte.valueOf(y))
     * </pre>
     *
     * @param  x the first {@code byte} to compare
     * @param  y the second {@code byte} to compare
     * @return the value {@code 0} if {@code x == y};
     *         a value less than {@code 0} if {@code x < y}; and
     *         a value greater than {@code 0} if {@code x > y}
     * @since 1.7
     */
    public static int compare(byte x, byte y) {
        return x - y;
    }

    /**
     * The number of bits used to represent a {@code byte} value in two's
     * complement binary form.
     *
     * @since 1.5
     */
    public static final int SIZE = 8;

    /** use serialVersionUID from JDK 1.1. for interoperability */
    private static final long serialVersionUID = -7183698231559129828L;

    @Override
    public PaniniBatchMessage<java.lang.Byte> panini$getMessage() {
        // TODO Auto-generated method stub
        return null;
    }
}
