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

package org.paninij.effects;

import javax.tools.JavaFileObject;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;

public class CapsuleEffect implements CallEffect {
	public final ClassSymbol caller;
	public final Symbol callee;
	public final MethodSymbol meth;

	// the following fields are for warning messages
	// the file position of this call
	public final int pos;
	// the line number of this call
	public final int line;
	// the line number of this call
	public final int col;
	// the file of this call
	public final String fileName;
	// the stmt that cause this effect
	public final JCMethodInvocation tree;
	// the source file that contains this effect
	public final JavaFileObject source_file;

	public CapsuleEffect(ClassSymbol caller, Symbol callee,
			MethodSymbol meth, int pos, int line, int col, String fileName,
			JCMethodInvocation tree, JavaFileObject source_file) {
		this.caller = caller;
		this.callee = callee;
		this.meth = meth;
		this.pos = pos;
		this.line = line;
		this.col = col;
		this.fileName = fileName;
		this.tree = tree;
		this.source_file = source_file;
	}

	public void printEffect() {
		System.out.println("CapsuleEffect caller = " + caller + "\tcallee = " +
				callee + "\tmethod = " + meth + "\tline = " + line + "\tpos = "
				+ pos);
	}

	public int hashCode() {
		return caller.hashCode() + callee.hashCode() + meth.hashCode() + pos;
	}

	public boolean equals(Object obj) {
		if (obj instanceof CapsuleEffect) {
			CapsuleEffect ce = (CapsuleEffect) obj;
			return caller.equals(ce.caller) && callee.equals(ce.callee) &&
			meth.equals(ce.meth) && pos == ce.pos;
		}
		return false;
	}

	public String effectToString() {
		String caller = this.caller.toString();
		String callee = this.callee.name.toString();
		String params = "";
		if (this.meth.params != null) {
			for (VarSymbol v : this.meth.params) {
				params = params + v.type.tsym.flatName() + " ";
			}
		}
		if (params.length() > 0)
			params = " " + params.substring(0, params.length() - 1);
		String meth = this.meth.owner + " " + this.meth.name + params;
		return "C" + caller + " " + callee + " " + meth + " " + pos + " " +
		line + " " + col + " " + fileName;
	}

	public int pos() { return pos; }
	public JCMethodInvocation call_stmt() { return tree; }
	public JavaFileObject source_file() { return source_file; }
}
