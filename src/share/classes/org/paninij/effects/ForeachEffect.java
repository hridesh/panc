package org.paninij.effects;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;

public class ForeachEffect implements CallEffect {
	public final CapsuleSymbol caller;
	public final Symbol callee;
	public final MethodSymbol meth;

	public ForeachEffect(CapsuleSymbol caller, Symbol callee,
			MethodSymbol meth) {
		this.caller = caller;
		this.callee = callee;
		this.meth = meth;
	}

	public void printEffect() {
		System.out.println("ForeachEffect caller = " + caller + "\tcallee = " +
				callee + "\tmethod = " + meth);
	}

	public int hashCode() {
		return caller.hashCode() + callee.hashCode() + meth.hashCode();
	}

	public boolean equals(Object obj) {
		if (obj instanceof ForeachEffect) {
			ForeachEffect fe = (ForeachEffect) obj;
			return caller.equals(fe.caller) && callee.equals(fe.callee) &&
			meth.equals(fe.meth);
		}
		return false;
	}

	@Override
	public String effectToString() {
		String caller=this.caller.toString();
		String callee=this.callee.owner+" "+this.callee.name;
		String meth="";
		String params = "";
		if(this.meth.params!=null){
			for(VarSymbol v : this.meth.params){
				params = params + v.type. tsym.flatName() + " ";
			}
		}
		if(params.length()>0)
			params = " " + params.substring(0, params.length() - 1);
		meth = meth + this.meth.owner+" " + this.meth.name+params;
		return "E"+caller+" " +callee+" " +meth;
	}
}