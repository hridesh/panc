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

package org.paninij.path;

import com.sun.tools.javac.code.Symbol;

/* Path = local */
public class Path_Var implements Path {
	public final Symbol base;

	// used for fc open method parameter.
	public final boolean pointToNewObject;

	public Path_Var(Symbol base, boolean pointToNewObject) {
		this.base = base;
		this.pointToNewObject = pointToNewObject;
	}

	public int hashCode() {
		if (base == null) { return -1; }
		return base.hashCode();
	}

	public boolean equals(Object o) {
		if (o instanceof Path_Var){ 
			Path_Var g = (Path_Var)o;
			if (base == null) { return g.base == null; }

			return base.equals(g.base);
		}
		return false;
	}

	public boolean isAffected_PathANDField(Symbol sf) {
		return isAffected_Path(sf);
	}

	public boolean isAffected_Path (Symbol sf) {
		return false;
	}

	public int length() {
		return 1;
	}

	public Symbol getField() {
		return null;
	}

	public String printPath() {
		return "local " + base;
	}

	public boolean isAffected_byUnanalyzablePathANDField() {
		return false;
	}

	public boolean isAffected_byUnanalyzablePath() {
		return false;
	}

	public boolean isAffected_Local(Symbol var) {
		return base.equals(var);
	}

	public Path switchBase(int base) {
		throw new Error("should not reach here");
	}
	public Path switchVarBase(Symbol base) {
		throw new Error("should not reach here");
	}
	public Path switchBaseWithPath(Path p) {
		throw new Error("should not reach here");
	}
	public Path switchBaseWithVar(Symbol l) {
		throw new Error("should not reach here");
	}

	public Path clonePath() {
		return new Path_Var(base, pointToNewObject);
	}

	public int getBase() { return -1; }

	public Path getBasePath() { return this; }
}