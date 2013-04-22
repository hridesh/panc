package org.paninij.path;

import com.sun.tools.javac.code.Symbol;

/* Path = Unknown */
public class Path_Unknown implements Path {
	public static Path_Unknown pfcUnknow = new Path_Unknown();
	private Path_Unknown() {}

	public boolean equals(Object o) {
		return o == pfcUnknow;
	}

	public boolean isAffected_PathANDField(Symbol sf) { return false; }
	public boolean isAffected_Path(Symbol sf) { return false; }
	public int length() { return -95; }
	public Symbol getField() { return null; }
	public String printPath() { return "unknown"; }
	public boolean isAffected_byUnanalyzablePathANDField() { return false; }
	public boolean isAffected_byUnanalyzablePath() { return false; }
	public boolean isAffected_Local(Symbol var) { return false; }
	public int getBase() { return -1; }
	public Path switchBase(int base) { return pfcUnknow; }
	public Path switchBaseWithPath(Path p) { return pfcUnknow; }
	public Path switchBaseWithVar(Symbol l) { return pfcUnknow; }
	public Path clonePath() { return pfcUnknow; }
	public Path switchVarBase(Symbol base) { return pfcUnknow; }

	public int hashCode() { return -1; }
	public Path getBasePath() { return this; }
}