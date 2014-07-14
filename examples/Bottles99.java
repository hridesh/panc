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
 * Contributor(s): http://99-bottles-of-beer.net
 */

capsule Bottles99 {
    void run() {
	for(int i=99; i>1; i--) 
	    System.out.println(nonZero(i));
	System.out.println("1 bottle of beer on the wall, 1 bottle of beer.\n" +
		"Take one down and pass it around, no more bottles of beer on the wall.\n\n" +
		"No more bottles of beer on the wall, no more bottles of beer.\n" +
		"Go to the store and buy some more, 99 bottles of beer on the wall.");
    }
    private String nonZero(int num){
	return num + " bottles of beer on the wall, "+ num + " bottles of beer.\n" +
		"Take one down and pass it around, " + (num-1)+" bottles of beer on the wall.\n";
    }
}