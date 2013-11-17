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

/* This class is for array effect bound used only. */
public class Path_Literal implements Path {
	public int intLiteral;

	public Path_Literal(int intLiteral) { this.intLiteral = intLiteral; }

	public int hashCode() { return intLiteral; }

	public boolean equals(Object o) {
		if (o instanceof Path_Literal) {
			Path_Literal pl = (Path_Literal)o;
			return intLiteral == pl.intLiteral;
		}
		return false;
	}

	public String printPath() {
		String result = new String();
		result += intLiteral;
		return result;
	}

	public Path clonePath() { return new Path_Literal(intLiteral); }

	public boolean isAffected_PathANDField(Symbol sf) { return false; }
	public boolean isAffected_Path(Symbol sf) { return false; }
	public int length() { return 1; }
	public Symbol getField() { return null; }
	public boolean isAffected_byUnanalyzablePathANDField() { return false; }
	public boolean isAffected_byUnanalyzablePath() { return false; }
	public boolean isAffected_Local(Symbol var) { return false; }
	public Path switchBase(int base) { return null; }
	public Path switchBaseWithPath(Path p) { return null; }
	public Path switchBaseWithVar(Symbol l) { return null; }
	public Path switchVarBase(Symbol base) { return null; }

	public int getBase() {
		throw new Error("This class is for array effect bound used only.");
	}

	public Path getBasePath() { return new Path_Literal(intLiteral); }
	
}