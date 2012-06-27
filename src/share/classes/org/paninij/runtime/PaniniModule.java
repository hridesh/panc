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

import org.paninij.runtime.types.Panini$Duck;

public abstract class PaniniModule extends Thread {
   protected Object[] objects = new Object[10];
   protected volatile int head = 0, tail = 0, size = 0;
   protected final ReentrantLock queueLock = new ReentrantLock();
   
   protected final void extendQueue() {
       assert(size==objects.length);
       Object[] newObjects = new Object[objects.length+10];
       System.arraycopy(objects, head, newObjects, 0, objects.length-head);
       System.arraycopy(objects, 0, newObjects, objects.length-head, tail);
       head = 0; tail = size;        
       objects = newObjects;
   }        

   /**
    * Checks to ensure whether this module's queue can accomodate numElems 
    * number of elements, and if not extends it.
    * @param numElems 
    */
   protected final void ensureSpace(int numElems) {
    if (head < tail) 
    	if (objects.length + (head - tail) < numElems) 
    		if (size != 0) extendQueue(); 
    		else if (head - tail < numElems) 
    			if (size != 0) extendQueue();
   }

  	/**
  	 * Extracts and returns the first duck from the module's queue. 
  	 * This method blocks if there are no ducks in the queue.
  	 * 
  	 * precondition: it is assumed that the lock queueLock is held before
  	 *               calling this method.
  	 * 
  	 * @return the first available duck in the module's queue.
  	 */
  	protected final Panini$Duck get$Next$Duck() {
  			nomessages: while (this.size <= 0) 
  				try {
  					synchronized(queueLock) { queueLock.wait(); }
  				} catch (InterruptedException e) {
  					continue nomessages;
  				}

  			size--;
  			Panini$Duck d = (Panini$Duck) objects[head++];
  			if (head >= objects.length) head = 0;
  			return d;
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
  	
  	/**
  	 * Causes the current module to complete its remaining work and then cease execution.
  	 * 
  	 * Shutdown is allowed only if the client module has permission to modify this module.
  	 * 
  	 * If there is a security manager, its checkAccess method is called with this module 
  	 * as its argument. This may result in throwing a SecurityException.
  	 * 
  	 * @throws SecurityException - if the client module is not allowed to access this module.
  	 * 
  	 */
  	public final void shutdown () {
  		this.checkAccess();
   	org.paninij.runtime.types.Panini$Duck$Void d = new org.paninij.runtime.types.Panini$Duck$Void(-1);
    queueLock.lock();
    ensureSpace(1);
    size = size + 1;
    objects[tail++] = d;
    if (tail >= objects.length) tail = 0;
    queueLock.unlock();
  	}
  	
  	/**
  	 * Causes the current module to immediately cease execution. 
  	 * 
  	 * Shutdown is allowed only if the client module has permission to modify this module.
  	 * 
  	 * If there is a security manager, its checkAccess method is called with this module 
  	 * as its argument. This may result in throwing a SecurityException.
  	 * 
  	 * @throws SecurityException - if the client module is not allowed to access this module.
  	 * 
  	 */
  	public final void exit () {
  		this.checkAccess();
   	org.paninij.runtime.types.Panini$Duck$Void d = new org.paninij.runtime.types.Panini$Duck$Void(-2);
    queueLock.lock();
    ensureSpace(1);
    size = size + 1;
    objects[tail++] = d;
    if (tail >= objects.length) tail = 0;
    queueLock.unlock();
  	}
  	
}