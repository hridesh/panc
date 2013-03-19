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
package org.paninij.comp;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.*;

import com.sun.tools.javac.parser.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.code.Flags;

import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

public class CapsuleAnnotationProcessor {

	Names names;
	TreeMaker make;
	ParserFactory parserFactory;

	public CapsuleAnnotationProcessor(Names names, TreeMaker make, ParserFactory parserFactory) {
		this.names = names;
		this.make = make;
		this.parserFactory = parserFactory;
	}
	
	public List<JCAnnotation> createCapsuleAnnotation(long flag, JCCapsuleDecl capsule){
		List<JCAnnotation> ann = List.<JCAnnotation>nil();
		if(flag == Flags.SERIAL)
			ann = List.<JCAnnotation>of(make.Annotation(make.Ident(names.fromString("CapsuleKind")), 
					List.<JCExpression>of(make.Literal("SERIAL"))), make.Annotation(make.Ident(names.fromString("PaniniCapsuleDeclSequential")), 
							List.<JCExpression>of(make.Assign(make.Ident(names.fromString("params")), make.Literal(capsule.params.toString())))));
		else if(flag == Flags.MONITOR)
			ann = List.<JCAnnotation>of(make.Annotation(make.Ident(names.fromString("CapsuleKind")), 
					List.<JCExpression>of(make.Literal("MONITOR"))), make.Annotation(make.Ident(names.fromString("PaniniCapsuleDeclSynchronized")), 
							List.<JCExpression>of(make.Assign(make.Ident(names.fromString("params")), make.Literal(capsule.params.toString())))));
		else if(flag == Flags.ACTIVE)
			ann = List.<JCAnnotation>of(make.Annotation(make.Ident(names.fromString("CapsuleKind")), 
					List.<JCExpression>of(make.Literal("ACTIVE"))), make.Annotation(make.Ident(names.fromString("PaniniCapsuleDeclThread")), 
							List.<JCExpression>of(make.Assign(make.Ident(names.fromString("params")), make.Literal(capsule.params.toString())))));
		else if(flag == Flags.TASK)
			ann = List.<JCAnnotation>of(make.Annotation(make.Ident(names.fromString("CapsuleKind")), 
					List.<JCExpression>of(make.Literal("TASK"))), make.Annotation(make.Ident(names.fromString("PaniniCapsuleDeclTask")), 
							List.<JCExpression>of(make.Assign(make.Ident(names.fromString("params")), make.Literal(capsule.params.toString())))));
		else 
			throw new AssertionError("Not a capsuleKind flag");
		return ann;
	}

	public void translate(CapsuleSymbol e, Attribute.Compound annotation) {
		String paramsString = "(" + annotation.values.get(0).snd.getValue() + ")";
		JavacParser parser = (JavacParser)parserFactory.newParser(paramsString, false, false, false);
		List<JCVariableDecl> params = parser.capsuleParameters();
		e.capsuleParameters = params;
	}

}