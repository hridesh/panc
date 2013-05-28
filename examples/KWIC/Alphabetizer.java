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

capsule Alphabetizer(CircularShifter shifter_){
	
	int sorted_[] = null; //Sorted indices of lines

	/**
	 * Sorts all lines from the shifter.
	 * @param shifter the source of lines
	 */
	void alpha(){
		sorted_ = new int[shifter_.getLineCount().value()];
		for(int i = 0; i < sorted_.length; i++)
			sorted_[i] = i;

		for(int i = (sorted_.length / 2 - 1); i >= 0; i--) // heap sort algorithm - create heap
			siftDown(i, sorted_.length);

		for(int i = (sorted_.length - 1); i >= 1; i--){ // remove the root, recreate heap
			int tmp = sorted_[0];
			sorted_[0] = sorted_[i];
			sorted_[i] = tmp;
			siftDown(0, i);      
		}
	}

	/**
	 * This method builds and reconstucts the heap for the heap sort algorithm.
	 * @param root heap root
	 * @param bottom heap bottom
	 */
	void siftDown(int root, int bottom){    
		int max_child = root * 2 + 1;

		while(max_child < bottom){
			if((max_child + 1) < bottom)
				if(shifter_.getLineAsString(sorted_[max_child + 1]).value().compareTo(
						shifter_.getLineAsString(sorted_[max_child]).value()) > 0)
					max_child++;

			if(shifter_.getLineAsString(sorted_[root]).value().compareTo(
					shifter_.getLineAsString(sorted_[max_child]).value()) < 0){
				int tmp = sorted_[root];
				sorted_[root] = sorted_[max_child];
				sorted_[max_child] = tmp;
				root = max_child;
				max_child = root * 2 + 1;
			}else
				break;
		}    
	}

	/**
	 * Gets the line from the specified position.
	 * String array representing the line is returned.
	 * @param line line index
	 * @return String[]
	 */
	Strings getLine(int line){
		return shifter_.getLine(sorted_[line]);
	}

	/**
	 * Gets the line from the specified position.
	 * String representing the line is returned.
	 * @param line line index
	 * @return String[]
	 */
	StringC getLineAsString(int line){
		return shifter_.getLineAsString(sorted_[line]);
	}

	IntegerC getLineCount(){
		return shifter_.getLineCount();
	}
}