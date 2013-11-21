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
 * Contributor(s): Lorand Szakacs
 */

/*
 * @test
 * @summary Ensure that the compiler can handle all types.
 * @compile TBatchSmokeAllTypes.java
 */
class Number{
    int getN() {
        return 42;
    }
}
capsule Test {}

capsule TBatchSmokeAllTypes {
	design {
		Test test;
		test();
	}
	
	void run() {
        test -> {
            System.out.println("executing batch with void return type");
        };
        
        Number n = test -> {
           return new Number();
        };
        System.out.println("Number: " + n.getN());

        String str = test -> {
            return "ultimate String";
        };
        System.out.println("String: " + str);
        
        long l = test -> {
            return (long)42;
        };
        System.out.println("long: " + l);
        
        int i = test -> {
            return 42;
        };
        System.out.println("int: " + i);
        
        short s = test -> {
            return (short)42;
        };
        System.out.println("short: " + s);
        
        byte b = test -> {
            return (byte)11;
        };
        System.out.println("byte: " + b);
        
        double d = test -> {
            return (double)42;
        };
        System.out.println("double: " + d);
        
        float f = test -> {
            return (float)42;
        };
        System.out.println("float: " + f);
        
        char c = test -> {
            return 'a';
        };
        System.out.println("char: " + c);
        
        boolean bool = test -> {
            return true;
        };
        System.out.println("boolean: " + bool);
        
        
	}
}