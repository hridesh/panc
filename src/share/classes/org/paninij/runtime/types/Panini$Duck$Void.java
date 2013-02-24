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

package org.paninij.runtime.types;

public class Panini$Duck$Void implements Panini$Duck<Void> {

	public Panini$Duck$Void(int messageId) {
		super();
		this.messageId = messageId;
	}
		
	public final Void panini$get(){
        while (redeemed == false) try {
            synchronized (this) {
                while (redeemed == false) wait();
            }
        } catch (InterruptedException e) {}
		return null;
	}
	
	public final Void value() {
		if (redeemed == false) this.panini$get();
		return null;
	}

	public void panini$finish(Void t) {
        synchronized (this) {
            redeemed = true;
            notify();
        }
	}

	public final int panini$message$id() {
		return this.messageId;
	}

	@SuppressWarnings("unused")
	private Panini$Duck$Void() { messageId = -1; }
	
    private final int messageId;
    private boolean redeemed = false;
}
