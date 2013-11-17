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

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;

/* Path = Path.f */
public class Path_Compound implements Path {
	public Path base;
	public Symbol field;

	public Path_Compound(Path base, Symbol field) {
		this.base = base;
		this.field = field;
	}

	public int hashCode() {
		return base.hashCode() + field.hashCode();
	}

	public boolean equals(Object o) {
		if (o instanceof Path_Compound){ 
			Path_Compound g = (Path_Compound)o;
			return base.equals(g.base) && field.equals(g.field);
		}
		return false;
	}

	public boolean isAffected_PathANDField(Symbol sf) {
		return base.isAffected_PathANDField(sf) || sf.equals(field);
	}

	public boolean isAffected_Path(Symbol sf) {
		return base.isAffected_PathANDField(sf);
	}

	public int length() {
		return base.length() + 1;
	}

	public Symbol getField() {
		return field;
	}

	public String printPath() {
		String result = base.printPath();
		result += "."+field;
		return result;
	}

	public boolean isAffected_byUnanalyzablePathANDField() {
		if((field.flags_field & Flags.FINAL) != 0) {
			return base.isAffected_byUnanalyzablePathANDField();
		}
		return true;
	}

	public boolean isAffected_byUnanalyzablePath() {
		return base.isAffected_byUnanalyzablePathANDField();
	}

	public boolean isAffected_Local(Symbol var) {
		return base.isAffected_Local(var);
	}

	public int getBase() { return base.getBase(); }

	public Path switchBase(int base) {
		return new Path_Compound(this.base.switchBase(base), field);
	}

	public Path switchBaseWithPath(Path p) {
		return new Path_Compound(this.base.switchBaseWithPath(p), field);
	}

	public Path switchBaseWithVar(Symbol l) {
		return new Path_Compound(this.base.switchBaseWithVar(l), field);
	}

	public Path clonePath() {
		return new Path_Compound(base.clonePath(), field);
	}

	public Path switchVarBase(Symbol base) {
		return new Path_Compound(this.base.switchVarBase(base), field);
	}

	public Path getBasePath() { return base.getBasePath(); }
}