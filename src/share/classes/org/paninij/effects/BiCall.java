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
 * Contributor(s): Yuheng Long
 */

package org.paninij.effects;

public class BiCall {
	public final CallEffect ce1;
	public final CallEffect ce2;

	// Indicate whether the call can be of the same index.
	public boolean notsameindex = false;

	public BiCall(CallEffect ce1, CallEffect ce2) {
		this.ce1 = ce1;
		this.ce2 = ce2;
	}

	public int hashCode() { return ce1.hashCode() + ce2.hashCode(); }

	public boolean equals(Object o) {
		if (o instanceof BiCall) { 
			BiCall g = (BiCall)o;

			return ce1.equals(g.ce1) && ce2.equals(g.ce2) &&
			notsameindex == g.notsameindex;
		}
		return false;
	}

	public void printCalls(String deliminator) {
		ce1.printEffect();
		System.out.print(deliminator);
		ce2.printEffect();
		System.out.println(deliminator + "\tnotsameindex = " + notsameindex);
	}
}
