package org.paninij.effects.analysis;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;

public class CapsuleEffect implements CallEffect {
	public final CapsuleSymbol caller;
	public final Symbol callee;
	public final MethodSymbol meth;

	public CapsuleEffect(CapsuleSymbol caller, Symbol callee,
			MethodSymbol meth) {
		this.caller = caller;
		this.callee = callee;
		this.meth = meth;
	}

	public void printEffect() {
		System.out.println("CapsuleEffect caller = " + caller + "\tcallee = " +
				callee + "\tmethod = " + meth);
	}

	public int hashCode() {
		return caller.hashCode() + callee.hashCode() + meth.hashCode();
	}

	public boolean equals(Object obj) {
		if (obj instanceof CapsuleEffect) {
			CapsuleEffect ce = (CapsuleEffect) obj;
			return caller.equals(ce.caller) && callee.equals(ce.callee) &&
			meth.equals(ce.meth);
		}
		return false;
	}
}