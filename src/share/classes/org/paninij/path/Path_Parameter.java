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

/* Path = id */
public class Path_Parameter implements Path {
	public Symbol base;
	public int id;

	public Path_Parameter(Symbol base, int id) {
		this.base = base;
		this.id = id;
	}

	public int hashCode() {
		return id;
	}

	public boolean equals(Object o) {
		if (o instanceof Path_Parameter){ 
			Path_Parameter g = (Path_Parameter)o;
			return /* base == g.base && */ id == g.id;
		}
		return false;
	}

	public boolean isAffected_PathANDField(Symbol sf) {
		return false;
	}

	public boolean isAffected_Path (Symbol sf) {
		return false;
	}

	public int getLocal() {
		return id;
	}

	public int length() {
		return 1;
	}

	public Symbol getField() {
		return null;
	}

	public String printPath() {
		if (base != null) {
			return "parameter " + id + "(" + base + ")";
		}
		return "parameter " + id;
	}

	public boolean isAffected_byUnanalyzablePathANDField() {
		return false;
	}

	public boolean isAffected_byUnanalyzablePath() { return false; }

	public Path switchBase(int base) {
		return new Path_Parameter(null, base); 
	}

	public Path switchBaseWithPath(Path p) {
		return p.clonePath();
	}

	public Path switchBaseWithVar(Symbol l) {
		return new Path_Var(l, false);
		// throw new Error();
	}

	public boolean isAffected_Local(Symbol var) {
		if (base == null) { return false; }
		return base.equals(var);
	}

	public Path clonePath() {
		return new Path_Parameter(base, id);
	}

	public Path switchVarBase(Symbol base) {
		// throw new Error();
		return new Path_Var(base, false);
	}

	public int getBase() { return id; }
	public Path getBasePath() { return this; }
}