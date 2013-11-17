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

/* Path = Path[index] */
public class Path_Array implements Path {
	public Path base;
	// TODO add index path

	public Path_Array(Path base) {
		this.base = base;
	}

	public int hashCode() {
		return base.hashCode();
	}

	public boolean equals(Object o) {
		if (o instanceof Path_Array) { 
			Path_Array g = (Path_Array)o;
			return base.equals(g.base);
		}
		return false;
	}

	public boolean isAffected_PathANDField(Symbol sf) {
		return base.isAffected_PathANDField(sf);
	}

	public boolean isAffected_Path(Symbol sf) {
		return base.isAffected_PathANDField(sf);
	}

	public int length() {
		return base.length() + 1;
	}

	public Symbol getField() { return null; }

	public String printPath() {
		String result = base.printPath();
		return result;
	}

	public boolean isAffected_byUnanalyzablePathANDField() {
		return base.isAffected_byUnanalyzablePathANDField();
	}

	public boolean isAffected_byUnanalyzablePath() {
		return base.isAffected_byUnanalyzablePathANDField();
	}

	public boolean isAffected_Local(Symbol var) {
		return base.isAffected_Local(var);
	}

	public int getBase() { return base.getBase(); }

	public Path switchBase(int base) {
		return new Path_Array(this.base.switchBase(base));
	}

	public Path switchBaseWithPath(Path p) {
		return new Path_Array(this.base.switchBaseWithPath(p));
	}

	public Path switchBaseWithVar(Symbol l) {
		return new Path_Array(this.base.switchBaseWithVar(l));
	}

	public Path clonePath() {
		return new Path_Array(base.clonePath());
	}

	public Path switchVarBase(Symbol base) {
		return new Path_Array(this.base.switchVarBase(base));
	}

	public Path getBasePath() { return base.getBasePath(); }
}