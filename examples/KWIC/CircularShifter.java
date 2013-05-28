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
import lib.*;

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

capsule CircularShifter(LineStorage lines, LineStorage shifts_){

/**
* Produces all circular shifts of lines in a given set. Circular shifts
* are stored internally and can be queried by means of other methods. Note,
* that for each line the first circular shift is same as the original line.
* @param lines A set of lines
* @see #getChar
* @see #getCharCount
* @see #getWord
* @see #getWordCount
* @see #getLine
* @see #getLineCount
*/
 void setup(){
   for(int i = 0; i < lines.getLineCount().value(); i++){
     String[] line = lines.getLine(i).value();
     for(int j = 0; j < line.length; j++){
       shifts_.addEmptyLine();
       for(int k = j; k < (line.length + j); k++)
         shifts_.addWordInString(line[k % line.length], shifts_.getLineCount().value() - 1);
     }
   }
 }

/**
* Gets the character from the specified position in the specified word 
* in a particular line.
* @param position character index in the word
* @param word word index in the line
* @param line line index
* @return char
*/
 CharC getChar(int position, int word, int line){
   return shifts_.getChar(position, word, line);
 }

/**
* Gets the number of characters in this particular word.
* @param word word index in the line
* @param line line index
* @return int
*/
 IntegerC getCharCount(int word, int line){
   return shifts_.getCharCount(word, line);
 }

/**
* Gets the word from the specified position in a particular line
* String representing the word is returned.
* @param word word index in the line
* @param line line index
* @return String
*/
 StringC getWord(int word, int line){
   return shifts_.getWord(word, line);
 }

/**
* Gets the number of words in this particular line.
* @param line line index
* @return int
*/
 IntegerC getWordCount(int line){
   return shifts_.getWordCount(line);
 }

/**
* Gets the line from the specified position.
* String array representing the line is returned.
* @param line line index
* @return String[]
*/
 Strings getLine(int line){
   return shifts_.getLine(line);
 }

/**
* Gets the line from the specified position.
* String representing the line is returned.
* @param line line index
* @return String
*/
 StringC getLineAsString(int line){
   return shifts_.getLineAsString(line);
 }

/**
* Gets the number of lines.
* @return int
*/
 IntegerC getLineCount(){
   return shifts_.getLineCount();
 }
}