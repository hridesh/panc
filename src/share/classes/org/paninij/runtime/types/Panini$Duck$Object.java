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

public class Panini$Duck$Object implements Panini$Duck<Object> {

	public Panini$Duck$Object(int messageId) {
		super();
		this.messageId = messageId;
	}

	public final boolean equals(Object obj) {
		if(value == null) get();
		value.getClass(); 
		return value.equals(obj);
	}

	public final Class<? extends Object> getClass(Object obj) {
		if(value == null) get();
		return value.getClass();
	}

	public final int hashCode() {
		if(value == null) get();
		return value.hashCode();
	}

	public final String toString() {
		if(value == null) get();
		return value.toString();
	}
	
	@Override
	public final void panini$finish(Object t) {
		value = t;
		notifyAll();
	}

	private Object value = null;
	private final void get() {
		value_not_yet_set: 
			do {
			try {
				synchronized (this) {
					wait(0);
				}
			} catch (InterruptedException e) {
				continue value_not_yet_set;
			}
		} while (value == null);
	}

	public final int panini$message$id() {
		return this.messageId;
	}

	protected final DuckBarrier future = new DuckBarrier();
	private final int messageId;

	@SuppressWarnings("unused")
	private Panini$Duck$Object() {
		messageId = -1;
	}
	
	// TODO: semantics for wait, notify(), notifyAll
}
