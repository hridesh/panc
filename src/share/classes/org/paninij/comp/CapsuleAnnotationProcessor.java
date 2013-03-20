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

import java.util.Iterator;

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
	
	//Can only be called if capsule annotation is defined.
	public void setDefinedRun(JCCapsuleDecl capsule, boolean definedRun){
		for(JCAnnotation ann : capsule.mods.annotations){
			if(ann.toString().contains("PaniniCapsuleDecl")){
				for(JCExpression exp : ann.args){
					if(exp.getTag() == Tag.ASSIGN && ((JCAssign)exp).lhs.toString().equals("definedRun"))
						((JCAssign)exp).rhs = make.Literal(new Boolean(definedRun));
				}
			}
		}
	}
	
	public List<JCAnnotation> createCapsuleAnnotation(long flag, JCCapsuleDecl capsule){
		List<JCAnnotation> ann = List.<JCAnnotation>nil();
		if(flag == Flags.SERIAL)
			ann = List.<JCAnnotation>of(make.Annotation(make.Ident(names.fromString("CapsuleKind")), 
					List.<JCExpression>of(make.Literal("SERIAL"))), make.Annotation(make.Ident(names.fromString("PaniniCapsuleDeclSequential")), 
							List.<JCExpression>of(
									make.Assign(make.Ident(names.fromString("params")), make.Literal(capsule.params.toString())),
									make.Assign(make.Ident(names.fromString("definedRun")), make.Literal(new Boolean(false)))
									)));
		else if(flag == Flags.MONITOR)
			ann = List.<JCAnnotation>of(make.Annotation(make.Ident(names.fromString("CapsuleKind")), 
					List.<JCExpression>of(make.Literal("MONITOR"))), make.Annotation(make.Ident(names.fromString("PaniniCapsuleDeclSynchronized")), 
							List.<JCExpression>of(
									make.Assign(make.Ident(names.fromString("params")), make.Literal(capsule.params.toString())),
									make.Assign(make.Ident(names.fromString("definedRun")), make.Literal(new Boolean(false)))
									)));
		else if(flag == Flags.ACTIVE)
			ann = List.<JCAnnotation>of(make.Annotation(make.Ident(names.fromString("CapsuleKind")), 
					List.<JCExpression>of(make.Literal("ACTIVE"))), make.Annotation(make.Ident(names.fromString("PaniniCapsuleDeclThread")), 
							List.<JCExpression>of(
									make.Assign(make.Ident(names.fromString("params")), make.Literal(capsule.params.toString())),
									make.Assign(make.Ident(names.fromString("definedRun")), make.Literal(new Boolean(false)))
									)));
		else if(flag == Flags.TASK)
			ann = List.<JCAnnotation>of(make.Annotation(make.Ident(names.fromString("CapsuleKind")), 
					List.<JCExpression>of(make.Literal("TASK"))), make.Annotation(make.Ident(names.fromString("PaniniCapsuleDeclTask")), 
							List.<JCExpression>of(
									make.Assign(make.Ident(names.fromString("params")), make.Literal(capsule.params.toString())),
									make.Assign(make.Ident(names.fromString("definedRun")), make.Literal(new Boolean(false)))
									)));
		else if(flag == Flags.INTERFACE)
			ann = List.<JCAnnotation>of(make.Annotation(make.Ident(names.fromString("PaniniCapsuleDeclInterface")), 
        			List.<JCExpression>of(
        					make.Assign(make.Ident(names.fromString("params")), make.Literal(capsule.params.toString())),
        					make.Assign(make.Ident(names.fromString("definedRun")), make.Literal(new Boolean(false)))
        					)));
		else 
			throw new AssertionError("Not a capsuleKind flag");
		return ann;
	}

	public void translate(CapsuleSymbol c, Attribute.Compound annotation) {
		fillInProcedures(c);
		String paramsString = "(" + annotation.values.get(0).snd.getValue() + ")";
		JavacParser parser = (JavacParser)parserFactory.newParser(paramsString, false, false, false);
		List<JCVariableDecl> params = parser.capsuleParameters();
		boolean definedRun = (Boolean)annotation.values.get(1).snd.getValue();
		c.capsuleParameters = params;
		c.definedRun = definedRun;
	}

	private void fillInProcedures(CapsuleSymbol c){
		Iterator<Symbol> iter = c.members().getElements().iterator();
		while(iter.hasNext()){
			Symbol s = iter.next();
			if(s instanceof MethodSymbol){
				CapsuleProcedure cp = new CapsuleProcedure(c, s.name, ((MethodSymbol)s).params);
	        	c.procedures.put((MethodSymbol)s, cp);
			}
		}
	}
}
