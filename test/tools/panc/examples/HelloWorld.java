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
 * Contributor(s): Hridesh Rajan
 */

/* 
 * @test
 * @summary Complie the HelloWorld example.
 * @compile HelloWorld.java
 * @compile/ref=HelloWorld.java.dot -graphs HelloWorld.java
 */

capsule Console () { //Capsule declaration
	void write(String s) { //Capsule procedure
		System.out.println(s); 
	}
}

capsule Greeter (Console c) { //Requires an instance of capsule Console to work
	void run(){                  //An autonomous capsule procedure
		c.write("Panini: Hello World!");          //Inter-capsule procedure call 
		long time = System.currentTimeMillis();
		c.write("Time is now: " + time);
	}
}

system HelloWorld {
	Console c; //Capsule instance declaration 
	Greeter g; //Another capsule instance declaration
	g(c);      //Wiring, connecting capsule instance g to c 
}
