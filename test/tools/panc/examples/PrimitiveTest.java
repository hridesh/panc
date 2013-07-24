/*
 * This file is part of the Panini project at Iowa State University.
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * For more details and the latest version of this code please see
 * http://paninij.org
 *
 * Contributor(s): Sean L. Mooney
 */

/*
 * @test
 * @summary Compile the auto-boxing primitives examples.
 * @compile PrimitiveTest.java

 */


capsule PrimitiveGiver(boolean bool, byte by, char ch, double d, float f, int i, long l, short s, String str){
	boolean getBoolean(){
		return bool;
	}

	byte getByte(){
		return by;
	}

	char getChar(){
		return ch;
	}

	double getDouble(){
		return d;
	}

	float getFloat(){
		return f;
	}

	int getInteger(){
		return i;
	}

	long getLong(){
		return l;
	}

	short getShort(){
		return s;
	}

	String getString(){
		return str;
	}
}

capsule PrimitiveGetter(PrimitiveGiver p){
	=>{
		System.out.println("Starting Primitive type procedure test...");
	}

	void run(){
		boolean bool = p.getBoolean();
		byte by = p.getByte();
		char ch = p.getChar();
		double d = p.getDouble();
		float f = p.getFloat();
		int i = p.getInteger();
		long l = p.getLong();
		short s = p.getShort();
		String str = p.getString();

		System.out.println("boolean: "+ bool);
		System.out.println("byte: "+ by);
		System.out.println("char: "+ ch);
		System.out.println("double: "+ d);
		System.out.println("float: "+ f);
		System.out.println("int: "+ i);
		System.out.println("long: "+ l);
		System.out.println("short: "+ s);
		System.out.println("String: "+ str);

		System.out.println("Ending primitive type procedure test...");
	}

}

capsule PrimitiveTest{
    design {
        PrimitiveGiver pgiver;
        PrimitiveGetter pgetter;
        pgiver(true, 127, 'C', 123.4d, 432.1f, 2147483647, 9223372036854775807L, 32767, "Success!");
        pgetter(pgiver);
    }
}
