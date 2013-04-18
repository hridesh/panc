package org.paninij.path;

import com.sun.tools.javac.code.Symbol;

/* Path = StaticPath|local|Path
 * StaticPath = C.f */
public interface Path {
	/* if encounterd unanalyzable code, e.g. unknown method call,
	 * is the path affected. */
	public boolean isAffected_byUnanalyzablePathANDField();
	public boolean isAffected_byUnanalyzablePath();

	public boolean isAffected_PathANDField(Symbol sf);
	public boolean isAffected_Path(Symbol sf);
	public boolean isAffected_Local(Symbol var);

	public int length();
	public Symbol getField();
	public int getBase();
	public Path getBasePath();

	public String printPath();

	public Path switchBase(int base);
	public Path switchVarBase(Symbol base);
	public Path switchBaseWithPath(Path p);
	public Path switchBaseWithVar(Symbol l);

	public Path clonePath();
}