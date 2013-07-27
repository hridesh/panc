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
 * Contributor(s): Eric Lin
 */

/*
 * @test
 * @summary Compile capsule with combinations of different argument signatures.
 * @compile MultiArgPrimDucks.java

 */
capsule PrimDuckTest () {
	int method0(){return 0;}
	
	int method0(double i){return 0;}
	
	int method0(double i, boolean b){return 0;}

	int method1(){return 0;}

	int method10(int i){return 0;}

	short method11(int i){return 0;}
	
	short method0(int i){return 0;}
	
	int method100(int[] i){return 0;}
}

