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
import com.sun.tools.javac.util.*;

import org.paninij.effects.*;

public class AnnotationProcessor extends Internal{

	Names names;
	TreeMaker make;
	ParserFactory parserFactory;
	private Log log;

	public AnnotationProcessor(Names names, TreeMaker make, ParserFactory parserFactory, Log log) {
		super(make, names);
		this.names = names;
		this.make = make;
		this.parserFactory = parserFactory;
		this.log = log;
	}

	//creates an annotationProcessor without a parserFactory.
	public AnnotationProcessor(Names names, TreeMaker make, Log log) {
		super(make, names);
		this.names = names;
		this.make = make;
		this.log = log;
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
			ann = List.<JCAnnotation>of(ann(id("CapsuleKind"), 
					List.<JCExpression>of(stringc("SERIAL"))), ann(id("PaniniCapsuleDeclSequential"), 
							List.<JCExpression>of(
									assign(id("params"), stringc(capsule.params.toString())),
									assign(id("definedRun"), falsev())
									)));
		else if(flag == Flags.MONITOR)
			ann = List.<JCAnnotation>of(ann(id("CapsuleKind"), 
					List.<JCExpression>of(stringc("MONITOR"))), ann(id("PaniniCapsuleDeclSynchronized"), 
							List.<JCExpression>of(
									assign(id("params"), stringc(capsule.params.toString())),
									assign(id("definedRun"), falsev())
									)));
		else if(flag == Flags.ACTIVE)
			ann = List.<JCAnnotation>of(ann(id("CapsuleKind"), 
					List.<JCExpression>of(stringc("ACTIVE"))), ann(id("PaniniCapsuleDeclThread"), 
							List.<JCExpression>of(
									assign(id("params"), stringc(capsule.params.toString())),
									assign(id("definedRun"), falsev())
									)));
		else if(flag == Flags.TASK)
			ann = List.<JCAnnotation>of(ann(id("CapsuleKind"), 
					List.<JCExpression>of(stringc("TASK"))), ann(id("PaniniCapsuleDeclTask"), 
							List.<JCExpression>of(
									assign(id("params"), stringc(capsule.params.toString())),
									assign(id("definedRun"), falsev())
									)));
		else if(flag == Flags.INTERFACE)
			ann = List.<JCAnnotation>of(ann(id("PaniniCapsuleDeclInterface"), 
        			List.<JCExpression>of(
        					assign(id("params"), stringc(capsule.params.toString())),
        					assign(id("definedRun"), falsev())
        					)));
		else 
			throw new AssertionError("Not a capsuleKind flag");
		return ann;
	}

	public void translate(CapsuleSymbol c, Attribute.Compound annotation) {
		if(parserFactory == null)
			throw new AssertionError("ParserFactory not available");
		fillInProcedures(c);
		if(annotation.values.size()!=2)//This number responds to the current implementation of CapsuleDecl annotations.
			log.error("capsule.incompatible.capsule.annotation", c.classfile.getName());
		for(Pair<MethodSymbol, Attribute> s: annotation.values){
			if(s.fst.name.toString().equals("params")){
				String paramsString = "(" + s.snd.getValue() + ")";
				JavacParser parser = (JavacParser)parserFactory.newParser(paramsString, false, false, false);
				List<JCVariableDecl> params = parser.capsuleParameters();
				c.capsuleParameters = params;
			}else if (s.fst.name.toString().equals("definedRun")){
				boolean definedRun = (Boolean)annotation.values.get(1).snd.getValue();
				c.definedRun = definedRun;
			}else{
				log.error("capsule.incompatible.capsule.annotation", c.classfile.getName());
			}
		}
	}
	
	public void setEffects(JCMethodDecl mdecl, EffectSet effectSet){
		setEffects(mdecl, effectSet.getEffects());
	}
	
	private List<JCExpression> effectsToExp(String[] effects){
		ListBuffer<JCExpression> effectsExp = new ListBuffer<JCExpression>();
		for(String s : effects){
			effectsExp.add(stringc(s));
		}
		return effectsExp.toList();
	}
	
	public void setEffects(JCMethodDecl mdecl, String[] effects){
		boolean annotated = false;
		for(JCAnnotation annotation : mdecl.mods.annotations){
			if(annotation.annotationType.toString().equals("Effects"))
				annotated = true;
		}
		if(!annotated){
			JCAnnotation ann = ann(id("Effects"), 
					List.<JCExpression>of(
							assign(id("effects"), make.NewArray(null, List.<JCExpression>nil(), 
									effectsToExp(effects)))
							));
			ann.setType(mdecl.sym.type.getReturnType());
			mdecl.mods.annotations = mdecl.mods.annotations.append(ann);
		}
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
