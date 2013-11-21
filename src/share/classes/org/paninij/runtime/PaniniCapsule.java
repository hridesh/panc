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

import org.paninij.runtime.types.Panini$Duck;

public interface PaniniCapsule{
    
    /**
     * All capsules support the batch message operation. Therefore
     * 0 will be the universal constant for this operation.
     */
    public final int panini$methodConst$BatchMessage = 0;
   
  	/**
  	 * Causes the current capsule to sleep (temporarily cease execution) 
  	 * for the specified number of milliseconds, subject to the precision 
  	 * and accuracy of system timers and schedulers. The capsule does not 
  	 * lose ownership of any monitors.
  	 * 
  	 * @param millis the length of time to sleep in milliseconds
  	 * @throws IllegalArgumentException - if the value of millis is negative
  	 * 
  	 */
  	public void yield (long millis);  	
  	
  	/**
  	 * Causes the current capsule to disconnect from its parent. On disconnecting
  	 * from all its parents, a terminate call is made to shutdown the capsule
  	 * running thread. This is part of automatic garbage collection of capsules.
  	 */
  	public void panini$disconnect();
  	
  	/**
  	 * Causes the current capsule to immediately cease execution. 
  	 * 
  	 * Shutdown is allowed only if the client capsule has permission to modify this capsule.
  	 * 
  	 * If there is a security manager, its checkAccess method is called with this capsule 
  	 * as its argument. This may result in throwing a SecurityException.
  	 * 
  	 * @throws SecurityException - if the client capsule is not allowed to access this capsule.
  	 * 
  	 */
  	public void exit ();
  	
  	/**
  	 * When invoked the client will have to provide the concrete type of the duck that
  	 * is to be returned for the 'DuckType'. This is required so that type checking
  	 * can go smoothly.
  	 * 
  	 * @param returnedDuck
  	 * @return
  	 */
  	public <FunType, DuckType extends Panini$Duck<FunType>> DuckType runBatch(DuckType returnedDuck);
  	
  	
  	public void start();
  	public void join() throws java.lang.InterruptedException; 
}