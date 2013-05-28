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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.StringTokenizer;

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
 *  Input capsule is responsible for reading and parsing the content of 
 *  a KWIC input file. The format of the KWIC input file is as follows:
 *  <ul>
 *  <li>Lines are separated by the line separator character(s) (on Unix '\n', on Windows '\r\n')
 *  <li>Each line consists of a number of words. Words are delimited by any number and combination
 *  of the space chracter (' ') and the horizontal tabulation chracter ('\t').
 *  </ul>
 *  Data is parsed and stored in memory as an instance of the LineStorage class:
 *  <ul>
 *  <li>All line separators are removed from the data; for each new line in the file a new line
 *  is added to the LineStorage instance
 *  <li>All horizontal tabulation word delimiters are removed
 *  <li>All space character word delimiters are removed
 *  <li>From characters between any two word delimiters a new string is created; the new string
 *  is added as a word to a particular line.
 *  </ul>
*/

capsule Input (LineStorage line_storage){	
 void parse(String file) {
    try{
      BufferedReader reader = new BufferedReader(new FileReader(file));

      String line = reader.readLine();
      while(line != null){
        StringTokenizer tokenizer = new StringTokenizer(line); // " \t\n\r\f" are delimiter character
        if(tokenizer.countTokens() > 0)
          line_storage.addEmptyLine();
        while(tokenizer.hasMoreTokens())
          line_storage.addWord(tokenizer.nextToken(), line_storage.getLineCount() - 1);
        line = reader.readLine();
      }
      
    }catch(FileNotFoundException exc){
      exc.printStackTrace();
      System.err.println("KWIC Error: Could not open " + file + "file.");
      System.exit(1);
    }catch(IOException exc){
      exc.printStackTrace();
      System.err.println("KWIC Error: Could not read " + file + "file.");
      System.exit(1);
    }
  }
}