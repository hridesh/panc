package org.paninij.effects;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;

public class CapsuleEffect implements CallEffect {
	public final CapsuleSymbol caller;
	public final Symbol callee;
	public final MethodSymbol meth;
	// the file position of this call
	public final int pos;
	// the line number of this call
	public final int line;
	// the file of this call
	public final String fileName;

	public CapsuleEffect(CapsuleSymbol caller, Symbol callee,
			MethodSymbol meth, int pos, int line, String fileName) {
		this.caller = caller;
		this.callee = callee;
		this.meth = meth;
		this.pos = pos;
		this.line = line;
		this.fileName = fileName;
	}

	public void printEffect() {
		System.out.println("CapsuleEffect caller = " + caller + "\tcallee = " +
				callee + "\tmethod = " + meth + "\tline = " + line);
	}

	public int hashCode() {
		return caller.hashCode() + callee.hashCode() + meth.hashCode();
	}

	public boolean equals(Object obj) {
		if (obj instanceof CapsuleEffect) {
			CapsuleEffect ce = (CapsuleEffect) obj;
			return caller.equals(ce.caller) && callee.equals(ce.callee) &&
			meth.equals(ce.meth) && pos == ce.pos;
		}
		return false;
	}
}