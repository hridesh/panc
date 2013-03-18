package com.sun.tools.javac.code;
import com.sun.tools.javac.tree.*;
import java.util.List;

public class CapsuleProcedure {
	public boolean isFresh;
	public boolean isCommunitive;
	Symbol.CapsuleSymbol owner;
	List<JCTree.JCVariableDecl > params;
	Type restype;
	
	public CapsuleProcedure(){
	}
	
	public CapsuleProcedure(Symbol.CapsuleSymbol owner, List<JCTree.JCVariableDecl> params){
		this.owner = owner;
		this.params = params;
	}
}
