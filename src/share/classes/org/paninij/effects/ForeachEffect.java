package org.paninij.effects;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;

public class ForeachEffect implements CallEffect {
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
	

	public ForeachEffect(ClassSymbol caller, Symbol callee,
			MethodSymbol meth, int pos, int line, int col, String fileName) {
		this.caller = caller;
		this.callee = callee;
		this.meth = meth;
		this.pos = pos;
		this.line = line;
		this.col = col;
		this.fileName = fileName;
	}

	public void printEffect() {
		System.out.println("ForeachEffect caller = " + caller + "\tcallee = " +
				callee + "\tmethod = " + meth + "\tline = " + line + "\t" + pos
				+ pos);
	}

	public int hashCode() {
		return caller.hashCode() + callee.hashCode() + meth.hashCode() + pos;
	}

	public boolean equals(Object obj) {
		if (obj instanceof ForeachEffect) {
			ForeachEffect fe = (ForeachEffect) obj;
			return caller.equals(fe.caller) && callee.equals(fe.callee) &&
			meth.equals(fe.meth)  && pos == fe.pos;
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
		return "E" + caller + " " + callee + " " + meth + " " + pos + " "
				+ line + " " + col + " " + fileName;
	}

	public int pos() { return pos; }
}