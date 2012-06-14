package org.paninij.runtime;

import java.util.concurrent.locks.ReentrantLock;

public abstract class PaniniModule extends Thread { // Panini$Module, will go in standard library
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
}