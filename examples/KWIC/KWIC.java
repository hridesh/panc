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

/**
 *  The Control capsule controls all other capsules in the KWIC system
 *  to achieve the desired functionality. 
*/
capsule Control (){
	design {
		LineStorage lines; LineStorage shifts;
        	Input input; CircularShifter shifter;
        	Alphabetizer alphabetizer; Output output; Control control;
        	String file = "../shaks12.txt";
        	alphabetizer(shifter);
        	shifter(lines,shifts);
        	input(lines);
        	output(alphabetizer);
	}

/**
 * Parses the data, makes shifts and sorts them. At the end prints the
 * sorted shifts.
 * @param file name of the input file
 */
 void run(){
    input.parse(file);
    shifter.setup(lines);
    alphabetizer.alpha();
    output.print();
  }

/**
 * Main function checks the command line arguments. The program expects 
 * exactly one command line argument specifying the name of the file 
 * that contains the data. If the program has not been started with 
 * proper command line arguments, main function exits
 * with an error message. Otherwise, a KWIC instance is created and program
 * control is passed to it.
 * @param args command line arguments
 */

  public static void main(String[] args){
    if(args.length != 1){
      System.err.println("KWIC Usage: java kwic.ms.KWIC file_name");
      System.exit(1);
    }

    KWIC kwic = new KWIC();
    kwic.execute(args[0]);
  }
}
