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
 * Contributor(s): Eric Lin, Hridesh Rajan
 */

capsule Helper () {
	Number fact(int current, int numIndents) { 
		String indents = "";
		for(int i=0; i<numIndents; i++) indents+= "\t";
		System.out.println(indents + "Computing factoring for " +  current);
		if(current == 0) 
			return new Number(0);
		Number recursiveTerm = fact(current-1, numIndents+1);
		System.out.println(indents + "\tThe recursive term is " + recursiveTerm);
		Number answer = new Number(current + recursiveTerm.v());
		System.out.println(indents + "Factorial of " + current + " is " + answer);
		return answer; 
	}
}

capsule Factorial {
	design {
  Helper h;
 }
	
	void run(){ 
		System.out.println(h.fact(10, 0).v()); 
	}
}

class Number{
	int number;
	Number(int number){ this.number = number; }
	int v(){ return number;}
	public String toString() { return "" + number; }
}