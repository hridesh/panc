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

/*** 
 * Classic KWIC system using the Panini language 
 * 
 * This implementation of the KWIC system is based on the example 
 * presented in the following paper. 
 * D. L. Parnas. 1972. On the criteria to be used in decomposing systems 
 * into modules. Commun. ACM 15, 12 (December 1972), 1053-1058. 
 * DOI=10.1145/361598.361623 http://doi.acm.org/10.1145/361598.361623
 * 
 */

capsule Output(Alphabetizer alphabetizer){
	/**
 * Prints the lines at the standard output.
 * @param alphabetizer source of the sorted lines
 */
 void print(){
    for(int i = 0; i < alphabetizer.getLineCount(); i++)
      System.out.println(alphabetizer.getLineAsString(i));
  }
}