package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.*;

import com.sun.tools.javac.parser.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Name;

import java.util.LinkedList;

public class CapsuleAnnotationTranslator {

	Names names;
    TreeMaker make;
    ParserFactory parserFactory;
    
    public CapsuleAnnotationTranslator(Names names, TreeMaker make, ParserFactory parserFactory) {
        this.names = names;
        this.make = make;
        this.parserFactory = parserFactory;
    }

    public void translate(ClassSymbol e, Attribute.Compound annotation) {
    	String paramsString = "(" + annotation.values.get(0).snd.getValue() + ")";
    	JavacParser parser = (JavacParser)parserFactory.newParser(paramsString, false, false, false);
    	List<JCVariableDecl> params = parser.capsuleParameters();
    	e.params = params;
    }
	
}
