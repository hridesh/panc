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