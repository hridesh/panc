package org.paninij.runtime;
import java.util.concurrent.locks.ReentrantLock;

public abstract class PaniniModule extends Thread {
   protected Object[] objects = new Object[10];
   protected int head = 0, tail = 0, size = 0;
   protected final ReentrantLock queueLock = new ReentrantLock();
   
   protected void extendQueue() {
       assert(size==objects.length);
       Object[] newObjects = new Object[objects.length+10];
       System.arraycopy(objects, head, newObjects, 0, objects.length-head);
       System.arraycopy(objects, 0, newObjects, objects.length-head, tail);
       head = 0; tail = objects.length;        
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