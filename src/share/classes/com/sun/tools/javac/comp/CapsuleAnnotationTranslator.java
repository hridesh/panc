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
 * Contributor(s): Eric Lin
 */
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
