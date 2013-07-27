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

// Short version: 
// To test compile this file and run this capsule.
//    $PATH_TO_PANC$/panc HelloWorld.java
//    $PATH_TO_PANINI$/panini HelloWorldShort

capsule HelloWorldShort() {
	void run() { 
		System.out.println("Panini: Hello World!");
	}
}

//Longer version that illustrates most Panini features.
//To test compile this file and run this capsule.
//$PATH_TO_PANC$/panc HelloWorld.java
//$PATH_TO_PANINI$/panini HelloWorld

signature Stream { //A signature declaration
	void write(String s);
}

capsule Console () implements Stream { //Capsule declaration
	void write(String s) { //Capsule procedure
		System.out.println(s); 
	}
}

capsule Greeter (Stream s) { //Requires an instance of Stream to work
	String message = "Panini: Hello World!"; // State declaration
	void greet(){                  //Capsule procedure
		s.write(message);  //Inter-capsule procedure call 
		long time = System.currentTimeMillis();
		s.write("Time is now: " + time);
	}
}

capsule HelloWorld() {
	design {       //Design declaration
		Console c; //Capsule instance declaration
		Greeter g; //Another capsule instance declaration
		g(c);      //Wiring, connecting capsule instance g to c
	}
	void run() { //An autonomous procedure
		g.greet(); //Inter-capsule procedure call 
	}
}
