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

import org.paninij.path.*;
import com.sun.tools.javac.code.Type;

public class ArrayEffect implements EffectEntry {
	public final Path path;
	public final Type type;

	public ArrayEffect(Path path, Type type) {
		this.path = path;
		this.type = type;
	}

	public ArrayEffect(Type type) {
		this.path = Path_Unknown.unknow;
		this.type = type;
	}

	public int hashCode() {
		return path.hashCode();
	}

	public boolean equals(Object obj) {
		if (obj instanceof ArrayEffect) {
			ArrayEffect rwe = (ArrayEffect) obj;
			return path.equals(rwe.path);
		}
		return false;
	}

	public void printEffect() {
		System.out.println("ArrayEffect base = " + path.printPath() +
				"\ttype = " + type);
	}

	public String effectToString() {
		return "A"+this.type.toString();
	}
}