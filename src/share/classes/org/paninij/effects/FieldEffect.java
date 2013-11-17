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

import org.paninij.path.Path;

import com.sun.tools.javac.code.Symbol;

public class FieldEffect implements EffectEntry {
	public final Path path; 
	public final Symbol f;

	public FieldEffect(Path path, Symbol f) {
		this.path = path;
		this.f = f;
	}

	public int hashCode() {
		return path.hashCode() + f.hashCode();
	}

	public boolean equals(Object obj) {
		if (obj instanceof FieldEffect) {
			FieldEffect rwe = (FieldEffect) obj;
			return f.equals(rwe.f) && path.equals(rwe.path);
		}
		return false;
	}

	public void printEffect(){
	    System.out.println("FieldEffect = " + path.printPath() + "\tf = " + f);
	}

	public String effectToString() {
		return "F"+this.f.owner+" "+this.f.name+ " "+ path.hashCode();
	}
}
