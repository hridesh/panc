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

public abstract class PaniniCapsuleSequential implements PaniniCapsule{
	public volatile int panini$ref$count;
	
	protected PaniniCapsuleSequential() {
		panini$ref$count = 0;
	}
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
  	public void yield (long millis) {
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
  	 * Causes the current capsule to disconnect from its parent. For sequential capsules,
  	 * a disconnect() on all connected capsules is called. This is part of automatic 
  	 * garbage collection of capsules.
  	 */
  	public synchronized void shutdown () {
  	}
  	
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
  	public final void exit () {
  	}

  	protected void panini$capsule$init(){}

    /**
     * Initialize the 'internal' system in a capsule.
     * <p>
     * Must be called <em>BEFORE</em> {@link #panini$capsule$init()}.
     */
	protected void panini$wire$sys() {}

  	public final void start(){
		panini$wire$sys();
  		panini$capsule$init();
  	}
  	
  	public final void panini$push(Object o){}
  	public final void join() throws java.lang.InterruptedException {} // TODO:

}
