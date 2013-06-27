package com.sun.tools.javac.code;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.*;

public class CapsuleProcedure {
	public Symbol.ClassSymbol owner;
	public List<Symbol.VarSymbol> params;
	public Name name;
	public Type restype;//unused
	public boolean isFresh;//unused
	public boolean isCommunitive;//unused
	
	public CapsuleProcedure(Symbol.ClassSymbol owner, Name name, List<Symbol.VarSymbol> params){
		this.owner = owner;
		this.params = params;
		this.name = name;
	}
}
