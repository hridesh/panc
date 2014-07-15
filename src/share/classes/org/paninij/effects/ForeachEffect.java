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
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;

import org.paninij.analysis.AnalysisUtil;

public class ForeachEffect implements CallEffect {
	public final ClassSymbol caller;
	public final Symbol callee;
	public final MethodSymbol meth;

	// indicate whether the index of the call is 0.
	// the index of cap[i] is i and cap[0] is 0
	public final boolean index;

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

	public ForeachEffect(ClassSymbol caller, Symbol callee, JCExpression index,
			MethodSymbol meth, int pos, int line, int col, String fileName,
			JCMethodInvocation tree, JavaFileObject source_file) {
		this(caller, callee, AnalysisUtil.isZero(index), meth, pos, line, col,
				fileName, tree, source_file);
	}

	public ForeachEffect(ClassSymbol caller, Symbol callee, boolean index,
			MethodSymbol meth, int pos, int line, int col, String fileName,
			JCMethodInvocation tree, JavaFileObject source_file) {
		this.caller = caller;
		this.callee = callee;
		this.index = index;
		this.meth = meth;
		this.pos = pos;
		this.line = line;
		this.col = col;
		this.fileName = fileName;
		this.tree = tree;
		this.source_file = source_file;
	}

	public void printEffect() {
		System.out.println("ForeachEffect caller = " + caller + "\tcallee = " +
				callee + "\tmethod = " + meth + "\tline = " + line + "\tpos = "
				+ pos);
	}

	public int hashCode() {
		return caller.hashCode() + callee.hashCode() + meth.hashCode() + pos;
	}

	public boolean equals(Object obj) {
		if (obj instanceof ForeachEffect) {
			ForeachEffect fe = (ForeachEffect) obj;
			return caller.equals(fe.caller) && callee.equals(fe.callee) &&
			index == fe.index && meth.equals(fe.meth)  && pos == fe.pos;
		}
		return false;
	}

	public String effectToString() {
		String caller = this.caller.toString();
		String callee = this.callee.owner + " " + this.callee.name;
		String meth = "";
		String params = "";
		if (this.meth.params != null) {
			for (VarSymbol v : this.meth.params) {
				params = params + v.type. tsym.flatName() + " ";
			}
		}
		if (params.length() > 0)
			params = " " + params.substring(0, params.length() - 1);
		meth = meth + this.meth.owner + " " + this.meth.name + params;
		return "E" + caller + " " + callee + " " + meth + " " + index + " " +
				pos + " " + line + " " + col + " " + fileName;
	}

	public int pos() { return pos; }
	public JCMethodInvocation call_stmt() { return tree; }
	public JavaFileObject source_file() { return source_file; }
}
