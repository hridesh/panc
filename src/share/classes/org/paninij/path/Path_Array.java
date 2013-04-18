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