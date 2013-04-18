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