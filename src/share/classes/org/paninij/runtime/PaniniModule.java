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
public interface PaniniModule{
   
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
  	public void yield (long millis);  	
  	
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
  	public void shutdown();
  	
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
  	public void exit ();
  	
  	public void start();
  	public void join() throws InterruptedException; 
}