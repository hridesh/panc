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
 * Classic Histogram problem using the Panini language 
 * 
 * For a detailed explanation see comments following this code.
 */

import java.io.*;

module Reader(String[] args, Bucket[] buckets) {
	
	void run() {
	 if(args.length == 0) process("shaks12.txt");
	 else 
	 	for(String fileName : args) 
	 		process(fileName);
	}

	private void process(String fileName) {
  try {
    FileInputStream stream =	new FileInputStream(new File(fileName));
    System.out.println("READER: input file " + fileName + " successfully opened. Starting processing ...");
    int r;
    while ((r = stream.read()) != -1) {
 			 buckets[(char) r].bump();
    }  
    System.out.println("READER: Reading complete. Asking buckets to print count.");
  } catch (IOException e) { System.out.println(e); }
		for(int i = 0; i < buckets.length; i++) 
			buckets[i].finish(i); 
  System.out.println("READER: work complete.");
	}

}

module Bucket(Printer p) {
		long count = 0;
		void bump() { count++; }
		void finish(int index) { System.out.println(count);p.print(index, count); }
}

module Printer() { 
	void print(int index, long count) { 
		System.out.print(index);
		System.out.print(": ");
		System.out.println(count);
		}
}

system Histogram (String[] args){
	Reader r; Bucket buckets[128]; Printer p;

	r(args, buckets);
	for(Bucket b : buckets) {
	    b(p);
	}
}


/*** 
 * Classic Histogram problem using the Panini language 
 */
 
/*
GOAL: The goal of this problem is to count the number of times each   
      ASCII character occurs on a page of text.                       
                                                                      
INPUT: ASCII text stored as an array of characters.                   
OUTPUT: A histogram with 128 buckets, one for each ascii character,   
        where each entry stores the number of occurrences of the      
        corresponding ascii character on the page.                    
                                                                      
ARCHITECTURE & DESIGN:                                                
                                                                      
  Step 1: Divide the problems into subproblems                        
    subproblems are: 1. read the ASCII text, 2. sort characters       
    into bit bucket, 3. output the bit bucket                         
                                                                      
  Step 2: Create modules and assign responsibilities to modules.      
    In assigning responsibility follow the information-hiding         
    principle. There are two design decisions that are likely to      
    change: input format and output format. Therefore, we must        
    hide these responsibility behind interface of separate modules.   
                                                                       
    This suggests three modules: Reader, Bucket, and Printer.         
                                                                      
     module Reader() { }                                              
     module Bucket() { }                                              
     module Printer() { }                                             
                                                                      
    We do not yet know the interconnection between these three        
    modules, but it seems to be the case that Reader ought to read    
    characters from the ASCII text and call Buckets to put characters 
    in the bucket. Finally, when characters are sorted, Bucket could  
    call the Printer to print count. This seems to suggest that the   
    Reader module ought to be able to reach Buckets and Bucket module 
    ought to be able to reach a Printer. We can use this knowledge    
    to refine our architecture and design.                            
                                                                      
     module Reader(Bucket[] buckets) { }                              
     module Bucket(Printer p) { }                                     
     module Printer() { }                                             
                                                                      
    The first line says that the Reader module is connected with a    
    set of Bucket modules. The second line says that the every Bucket 
    is connected with a printer module, and the third line says that  
    the Printer module is not connected to any other module.          
                                                                      
    We can now start specifying behavior of each of these modules.    
    The behavior of module Printer is fairly straightforward, given   
    a string it ought to display that string on Console.              
                                                                      
    module Printer () {                                               
     void print(String output) { System.out.println(output); }        
    }                                                                 
                                                                      
    The behavior of the module Bucket requires keeping track of the   
    the number of items in the bucket (since all items are the same). 
    In Panini, a module can declare states to keep track of such      
    pieces of information. A state declaration is syntactically same  
    as a field declaration in object-oriented languages, however, it  
    differs semantically in two ways: first, a state is private to a  
    a module (there are no public modifiers.), second, all the memory 
    locations that can be reached via this state are uniquely owned   
    the containing module. Other modules may not access it.           
                                                                      
    module Bucket(Printer p) {                                        
      long count = 0;                                                 
                                                                      
    To allow other modules to change its state, a module can provide  
    module procedures, procedures for short. A module procedure is    
    syntactically similar to methods in object-oriented languages,    
    however, they are different semantically in two ways: first, a    
    module procedures is by default public (although private helper   
    procedures can be declared using the private keyword), and second 
    a procedure invocation is guaranteed to be logically synchronous. 
    In some cases, Panini may be able to run procedures in parallel   
    to improve parallelism in Panini programs. Two example procedures 
    of the module Bucket are shown below.                             
                                                                      
    void bump() { count++; }                                          
    void finish(int index) { p.print(\"\"+ index + \":\" + count); }  
    }                                                                 
                                                                      
    Finally, the Reader module declares a procedure    
    run that reads the input array and sorts them into buckets.       
                                                                      
*/
