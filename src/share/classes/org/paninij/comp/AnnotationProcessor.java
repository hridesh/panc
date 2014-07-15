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
import com.sun.tools.javac.comp.*;

import com.sun.tools.javac.parser.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.code.Flags;

import com.sun.tools.javac.util.*;

import org.paninij.effects.*;
import org.paninij.path.*;

public class AnnotationProcessor extends Internal {
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
		for (List<JCAnnotation> l = capsule.mods.annotations; l.nonEmpty(); l = l.tail){
			JCAnnotation ann = l.head;
			if(ann.toString().contains("PaniniCapsuleDecl")){
				for (List<JCExpression> e = ann.args; e.nonEmpty(); e = e.tail){
					JCExpression exp = e.head;
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

	public void translateCapsuleAnnotations(ClassSymbol c, Attribute.Compound annotation) {
		if(parserFactory == null)
			throw new AssertionError("ParserFactory not available");
		fillInProcedures(c);
		if(annotation.values.size()!=2)//This number responds to the current implementation of CapsuleDecl annotations.
			log.error("capsule.incompatible.capsule.annotation", c.classfile.getName());
		for(Pair<MethodSymbol, Attribute> s: annotation.values){
			if(s.fst.name.toString().equals("params")){
				String paramsString = "(" + s.snd.getValue() + ")";
				fillInParams(c, paramsString);
			}else if (s.fst.name.toString().equals("definedRun")){
				boolean definedRun = (Boolean)annotation.values.get(1).snd.getValue();
				c.capsule_info.definedRun = definedRun;
			}else{
				log.error("capsule.incompatible.capsule.annotation", c.classfile.getName());
			}
		}
	}
	
	
	private  MethodSymbol findMethod(Symbol s, String [] signature){
		Iterator<Symbol> iter = s.members().getElements().iterator();
		MethodSymbol bestsofar = null;
		while(iter.hasNext()){
			Symbol member = iter.next();
			if(member instanceof MethodSymbol){
				if(member.name.toString().equals(signature[3])){
					if(signature.length>8 && ((MethodSymbol) member).params().size()==signature.length-8){
						for(int i=4; i < signature.length-4 ; i++){
							if(!((MethodSymbol) member).params().get(i-4).type.tsym.flatName().toString().equals(signature[i]))
								break;
							if(i == signature.length-5)
								bestsofar = (MethodSymbol) member;
						}
					}else
						bestsofar = (MethodSymbol) member;
				}
			}
		}
		return bestsofar;
	}
	
	/**
	 * Used to find a symbol from the members of a classSymbol
	 */
	private Symbol findField(Symbol s, String name){
		return findField((ClassSymbol)s, names.fromString(name));
	}

	private Symbol findField(ClassSymbol s, Name name) {
	    return s.members_field.lookup(name, VAR_FILTER).sym;
	}
	// where
      private static final Filter<Symbol> VAR_FILTER = new Filter<Symbol>() {
          public boolean accepts(Symbol s) {
              return s.kind == Kinds.VAR;
          }
      };
	
	/**
	 * This translates the value field of an effect annotation to an EffectSet
	 */
	private EffectSet translateEffects(String[] effects, Env<AttrContext> env,
			Resolve rs) {
		EffectSet es = new EffectSet();
		for (String s1 : effects) {
			String s = s1.substring(1, s1.length() - 1);
			if (s.equals("B")) {
				es.isBottom = true;
				return es;
			} else if (s.equals("T")) {
				es = new EffectSet(true);
			} else if (s.equals("F")) {
				es = new EffectSet(false);
			} else {
				String[] split;
				Symbol ownerSymbol;
				MethodSymbol m;
				EffectEntry effect = null;
				split = s.substring(2).split(" ");
				char c = s.charAt(1);
				switch (c) {
				case 'I':
					effect = new IOEffect(null, null);
					break;
				case 'F':// field effect owner name of symbol
					ownerSymbol = rs.findIdent(env, names.fromString(split[0]),
							Kinds.TYP);
					effect = new FieldEffect(new Path_Parameter(ownerSymbol,
							Integer.parseInt(split[2])), findField(ownerSymbol,
							split[1]));
					break;
				case 'C': // capsule effect
					ownerSymbol = rs.findIdent(env, names.fromString(split[0]),
							Kinds.TYP); // caller
					m = findMethod(rs.findIdent(env,
							names.fromString(split[2]), Kinds.TYP), split);
					effect = new CapsuleEffect( (ClassSymbol)ownerSymbol,
							findField(ownerSymbol, split[1]), m,
							Integer.parseInt(split[split.length - 4]),
							Integer.parseInt(split[split.length - 3]),
							Integer.parseInt(split[split.length - 2]),
							split[split.length - 1], null, null);
					break;
				case 'E': // foreacheffect
					ownerSymbol = rs.findIdent(env, names.fromString(split[0]),
							Kinds.TYP); // caller
					m = findMethod(rs.findIdent(env,
							names.fromString(split[2]), Kinds.TYP), split);
					effect = new ForeachEffect( (ClassSymbol)ownerSymbol,
							findField(ownerSymbol, split[1]),
							Boolean.parseBoolean(split[split.length - 5]), m,
							Integer.parseInt(split[split.length - 4]),
							Integer.parseInt(split[split.length - 3]),
							Integer.parseInt(split[split.length - 2]),
							split[split.length - 1], null, null);
					break;
				case 'A': // Array effect
					// m = findMethod(ownerSymbol, split);
					// es.add(es.methodEffect(m));
					break;
				default:
					Assert.error("Error when translating effects: unknown effect");
				}
				if (effect != null) {
					char c2 = s.charAt(0);
					switch (c2) {
					case 'R':
						es.read.add(effect);
						break;
					case 'W':
						es.write.add(effect);
						break;
					case 'C':
						es.calls.add((CallEffect) effect);
						break;
					default:
						Assert.error("Error when translating effects: unknown effect");
					}
				}
			}
		}
		return es;
	}
	
	@SuppressWarnings("rawtypes")
	public EffectSet translateEffectAnnotations(MethodSymbol m,
			Attribute.Compound annotation, Env<AttrContext> env, Resolve rs) {
		EffectSet es = new EffectSet();
		// check if its an Effects annotation?
		for (Pair<MethodSymbol, Attribute> pair : annotation.values) {
			if (pair.fst.name.toString().equals("effects")) {
				Object value = pair.snd.getValue();
				if (value instanceof List) {
					@SuppressWarnings("rawtypes")
					String[] effects = new String[((List) value).size()];
					for (int i = 0; i < ((List) value).size(); i++) {
						effects[i] = String.valueOf(((List) value).get(i));
					}
					es = translateEffects(effects, env, rs);
				}
			} else {
				log.error("capsule.incompatible.capsule.annotation",
						m.outermostClass().classfile.getName());
			}
		}
		return es;
	}
	
	/**
	 * Translates an effectSet to an annotation and add it to the Method.
	 */
	public void setEffects(JCMethodDecl mdecl, EffectSet effectSet){
		setEffects(mdecl, effectSet.effectsToStrings());
	}
	
	private List<JCExpression> effectsToExp(String[] effects){
		ListBuffer<JCExpression> effectsExp = new ListBuffer<JCExpression>();
		for(String s : effects){
			effectsExp.add(stringc(s));
		}
		return effectsExp.toList();
	}
	
	
	private void setEffects(JCMethodDecl mdecl, String[] effects) {
		boolean annotated = false;
		for (List<JCAnnotation> l = mdecl.mods.annotations; l.nonEmpty(); l = l.tail){
			JCAnnotation annotation = l.head;
			if (annotation.annotationType.toString().equals("Effects"))
				annotated = true;
		}
		if (!annotated) {
			JCAnnotation ann = ann(id("Effects"),
					List.<JCExpression> of(assign(id("effects"), make.NewArray(
							null, List.<JCExpression> nil(),
							effectsToExp(effects)))));
			ann.setType(mdecl.sym.type.getReturnType());
			mdecl.mods.annotations = mdecl.mods.annotations.append(ann);
		}
	}

	/**
	 * Parse the paramaters annotation, and fill in the paramater sym/type
	 * using the existing information from the class symbol's fields.
	 *
	 * Assumes this happens when a capsule is loaded from a class file.
	 * @param c
	 * @param paramsString
	 */
    private void fillInParams(ClassSymbol c, String paramsString) {
        fillInCapsuleParams(c, paramsString);
        fillInWiringSymbol(c);
    }
    //where
    private void fillInCapsuleParams(ClassSymbol c,
            String paramsString) {
        JavacParser parser = (JavacParser) parserFactory.newParser(paramsString, false, false, false);
        List<JCVariableDecl> params = parser.capsuleParameters();
        c.capsule_info.capsuleParameters = params;
    }
    private void fillInWiringSymbol(ClassSymbol c) {
        ListBuffer<Type> wts = new ListBuffer<Type>();
        for (List<JCVariableDecl> l = c.capsule_info.capsuleParameters;
                l.nonEmpty(); l = l.tail){
        	JCVariableDecl p = l.head;
            Symbol fs = findField(c, p.name);
            Type t = fs.type;
            Assert.checkNonNull(t, "No type for capsule " + c + " parameter " + p);
            wts.append(t);
            VarSymbol v = new VarSymbol(0, p.name, t, c);
            v.owner = c;
            p.sym = v;
        }

        //Create a wiring symbol from the parameter types.
        WiringSymbol wiringSym = new WiringSymbol(0, names.panini.Wiring,
                new org.paninij.code.Type.WiringType(wts.toList(), c), c);
        c.capsule_info.wiringSym = wiringSym;
    }
    //end fillInParams helpers

	private void fillInProcedures(ClassSymbol c){
		Iterator<Symbol> iter = c.members().getElements().iterator();
		while(iter.hasNext()){
			Symbol s = iter.next();
			if(s instanceof MethodSymbol){
				CapsuleProcedure cp = new CapsuleProcedure(c, s.name, ((MethodSymbol)s).params);
	        	c.capsule_info.procedures.put((MethodSymbol)s, cp);
			}
		}
	}
}
