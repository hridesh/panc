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