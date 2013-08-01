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

class Long{
	private long val;
	public Long(long l)	{
		val = l;		
	}
	
	public long value()	{
		return val;
	}
}

capsule Reader(String[] args, Bucket[] buckets, Printer p) {
	
	void read() {
		if(args.length == 0) process("shaks12.txt");
		else {
			for(String fileName : args)
				process(fileName);
		}
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
		Long[] results = foreach(Bucket b : buckets) b.getCount();
		for(int i = 0; i < results.length; i++) 
			p.print("" + i + ":" + results[i].value()); 
		System.out.println("READER: work complete.");
	}

}

capsule Bucket() {

	long count = 0;
	
	void bump() { count++; }
	
	Long getCount(){
		return new Long(count);
	}

}

capsule Printer() { 

	void print(String output) { 
		System.out.println(output); 
	}

}

capsule ForeachHistogram (String[] args){

	design {
		Reader r;
		Bucket buckets[128];
		Printer p;

		r(args, buckets, p);
	}
	
	void run() {
		r.read();
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

  Step 2: Create capsules and assign responsibilities to capsules.      
    In assigning responsibility follow the information-hiding         
    principle. There are two design decisions that are likely to      
    change: input format and output format. Therefore, we must        
    hide these responsibility behind interface of separate capsules.   

    This suggests three capsules: Reader, Bucket, and Printer.         

     capsule Reader() { }                                              
     capsule Bucket() { }                                              
     capsule Printer() { }                                             

    We do not yet know the interconnection between these three        
    capsules, but it seems to be the case that Reader ought to read    
    characters from the ASCII text and call Buckets to put characters 
    in the bucket. Finally, when characters are sorted, Reader could  
    call the Printer to print count. This seems to suggest that the   
    Reader capsule ought to be able to reach Buckets and Printer.
    We can use this knowledge to refine our architecture and design.                            

     capsule Reader(Bucket[] buckets, Printer p) { }                              
     capsule Bucket() { }                                     
     capsule Printer() { }                                             

    The first line says that the Reader capsule is connected with a    
    set of Bucket capsules and a Printer capsule. The second and third 
    lines say that the Bucket and Printer capsules, respectively,
    are not connected to any other capsule.          

    We can now start specifying behavior of each of these capsules.    
    The behavior of capsule Printer is fairly straightforward, given   
    a string it ought to display that string on Console.              

    capsule Printer () {                                               
     void print(String output) { System.out.println(output); }        
    }                                                                 

    The behavior of the capsule Bucket requires keeping track of the   
    the number of items in the bucket (since all items are the same). 
    In Panini, a capsule can declare states to keep track of such      
    pieces of information. A state declaration is syntactically same  
    as a field declaration in object-oriented languages, however, it  
    differs semantically in two ways: first, a state is private to a  
    a capsule (there are no public modifiers.), second, all the memory 
    locations that can be reached via this state are uniquely owned   
    the containing capsule. Other capsules may not access it.           

    capsule Bucket() {                                        
      long count = 0;                                                 

    To allow other capsules to change its state, a capsule can provide  
    capsule procedures, procedures for short. A capsule procedure is    
    syntactically similar to methods in object-oriented languages,    
    however, they are different semantically in two ways: first, a    
    capsule procedures is by default public (although private helper   
    procedures can be declared using the private keyword), and second 
    a procedure invocation is guaranteed to be logically synchronous. 
    In some cases, Panini may be able to run procedures in parallel   
    to improve parallelism in Panini programs. Two example procedures 
    of the capsule Bucket are shown below.                             

    void bump() { count++; }                                          
    long getCount() { return count; }  
    }                                                                 

    Finally, the Reader capsule declares a procedure    
    run that reads the input array and sorts them into buckets.       

 */
