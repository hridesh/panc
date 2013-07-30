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
 * Contributor(s): Eric Lin
 */
package org.paninij.runtime.types;

public class Panini$Duck$Array$Types implements Panini$Duck<Object>{
	private Object panini$wrapped = null;
    private final int panini$message$id = 0;
    private boolean panini$redeemed = false;
    
    public final void panini$finish(Object t) {
        synchronized (this) {
            panini$wrapped = t;
            panini$redeemed = true;
            notifyAll();
        }
    }
    
    public final int panini$message$id() {
        return this.panini$message$id;
    }
        
    public final Object panini$get() {
        while (panini$redeemed == false) try {
            synchronized (this) {
                while (panini$redeemed == false) wait();
            }
        } catch (InterruptedException e) {
        }
        return panini$wrapped;
    }
    
    public Object arrayValue() {
    	if (panini$redeemed == false) panini$get();
        return panini$wrapped;
    }
}
