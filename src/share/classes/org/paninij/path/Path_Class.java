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

/* Path = C */
public class Path_Class implements Path {
	public Symbol classSym;

	public Path_Class(Symbol classSym) {
		this.classSym = classSym;
	}

	public int hashCode() {
		return classSym.hashCode();
	}

	public boolean equals(Object o) {
		if (o instanceof Path_Class){ 
			Path_Class g = (Path_Class)o;
			return classSym.equals(g.classSym);
		}
		return false;
	}

	public boolean isAffected_PathANDField(Symbol sf) { return false; }
	public boolean isAffected_Path (Symbol sf) { return false; }
	public boolean isAffected_byUnanalyzablePathANDField() { return false; }
	public boolean isAffected_byUnanalyzablePath() { return false; }
	public boolean isAffected_Local(Symbol var) { return false; }
	public int length() { return 1; }

	public Symbol getField() { return null; }

	public String printPath() {
		return "PathFC_Class" + classSym;
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
		return new Path_Class(classSym);
	}

	public int getBase() { return -1; }
	public Path getBasePath() { return this; }
}