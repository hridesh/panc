package org.paninij.effects;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;

public class CapsuleEffect implements CallEffect {
	public final CapsuleSymbol caller;
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

	public CapsuleEffect(CapsuleSymbol caller, Symbol callee,
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
		String caller=this.caller.toString();
		String callee=this.callee.name.toString();
		String params = "";
		if(this.meth.params!=null){
			for(VarSymbol v : this.meth.params){
				params = params + v.type.tsym.flatName() + " ";
			}
		}
		if(params.length()>0)
			params = " " + params.substring(0, params.length() - 1);
		String meth=this.meth.owner+" " + this.meth.name+params;
		return "C" + caller + " " + callee + " " + meth + " " + pos + " "
				+ line + " " + col + " " + fileName;
	}

	public int pos() { return pos; }
}