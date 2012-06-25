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

package org.paninij.runtime;
import java.util.concurrent.locks.ReentrantLock;

public abstract class PaniniModule extends Thread {
   protected volatile Object[] objects = new Object[10];
   protected volatile int head = 0, tail = 0, size = 0;
   protected final ReentrantLock queueLock = new ReentrantLock();
   
   protected void extendQueue() {
       assert(size==objects.length);
       Object[] newObjects = new Object[objects.length+10];
       System.arraycopy(objects, head, newObjects, 0, objects.length-head);
       System.arraycopy(objects, 0, newObjects, objects.length-head, tail);
       head = 0; tail = size;        
       objects = newObjects;
   }        
   
   protected final boolean empty() { return size==0; }
   
   protected final void print() { 
       synchronized (System.out) {
           System.out.println("size: " + size);
           System.out.print("[");
           for (int i = 0; i < objects.length-1; i++) {
               if (i==head) System.out.print("!!H!!");
               if (i==tail) System.out.print("!!T!!");
               System.out.print(objects[i] + ", ");
           }
           if (objects.length>0) System.out.println(objects[objects.length-1] + "]");
           else System.out.println("]");
           
       }
   }
   
  	/**
  	 * Causes the current module to sleep (temporarily cease execution) 
  	 * for the specified number of milliseconds, subject to the precision 
  	 * and accuracy of system timers and schedulers. The module does not 
  	 * lose ownership of any monitors.
  	 * 
  	 * @param millis the length of time to sleep in milliseconds
  	 * @throws IllegalArgumentException - if the value of millis is negative
  	 * 
  	 */
  	protected void yield (long millis) {
  		if(millis < 0) throw new IllegalArgumentException();
  		try {
  			Thread.sleep(millis);
  			//TODO: this may also be a good place to introduce interleaving.
  		} catch (InterruptedException e) {
  			e.printStackTrace();
  			//TODO: What should be the semantics here? 
  		}
  	}  	
}