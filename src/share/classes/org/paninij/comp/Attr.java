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
 * Contributor(s): 
 */

package org.paninij.comp;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.TypeTags.INT;
import static com.sun.tools.javac.tree.JCTree.Tag.APPLY;
import static com.sun.tools.javac.tree.JCTree.Tag.ASSIGN;
import static com.sun.tools.javac.tree.JCTree.Tag.CAPSULEARRAY;
import static com.sun.tools.javac.tree.JCTree.Tag.EXEC;
import static com.sun.tools.javac.tree.JCTree.Tag.FOREACHLOOP;
import static com.sun.tools.javac.tree.JCTree.Tag.LT;
import static com.sun.tools.javac.tree.JCTree.Tag.MAAPPLY;
import static com.sun.tools.javac.tree.JCTree.Tag.PREINC;
import static com.sun.tools.javac.tree.JCTree.Tag.TYPEIDENT;
import static com.sun.tools.javac.tree.JCTree.Tag.VARDEF;

import java.util.HashMap;
import java.util.Map;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol.CapsuleSymbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.PaniniConstants;

public class Attr {
	// Visitor functions, dispatched here to separate Panini code
	
    public static void visitCapsuleDef(final JCCapsuleDecl tree, CapsuleInternal capsuleInternal, Enter enter,
    		com.sun.tools.javac.comp.Attr attr, Resolve rs, Env<AttrContext> env, Log log){
    	if (tree.needsDefaultRun){
    		List<JCClassDecl> wrapperClasses = capsuleInternal.generateClassWrappers(tree, env, rs);
    		enter.classEnter(wrapperClasses, env.outer);
    		//        	System.out.println(wrapperClasses);
    		attr.attribClassBody(env, tree.sym);
    		if((tree.sym.flags_field & TASK) !=0)
    			tree.computeMethod.body = capsuleInternal.generateTaskCapsuleComputeMethodBody(tree);
    		else
    			tree.computeMethod.body = capsuleInternal.generateThreadCapsuleComputeMethodBody(tree);
    	}
    	else
    		attr.attribClassBody(env, tree.sym);
    	for(JCTree def : tree.defs){
    		if(def.getTag() == Tag.METHODDEF){
    			for(JCVariableDecl param : ((JCMethodDecl)def).params){
    				if(param.type.tsym.isCapsule&&!((JCMethodDecl)def).name.toString().contains("$Original")){
    					log.error("procedure.argument.illegal", param, ((JCMethodDecl)def).name.toString(), tree.name.subSequence(0, tree.name.toString().indexOf("$")));
    				}
    			}

    		}else if(def.getTag() == Tag.VARDEF){
    				if(((JCVariableDecl)def).type.tsym.isCapsule)
    					((JCVariableDecl)def).mods.flags |= FINAL;
    		}
    	}
        /*if (doGraphs)
            effects.computeEffects(tree);*/
    }

    public static void visitSystemDef(final JCSystemDecl tree, TreeMaker make, Names names, Symtab syms, 
    		Resolve rs, Env<AttrContext> env, Log log, Annotate annotate, MemberEnter memberEnter, boolean doGraphs){
        /*if (doGraphs) {
            tree.sym.graphs = graphsBuilder.buildGraphs(tree);
            effects.substituteProcEffects(tree);
            ConsistencyCheck cc = 
                new ConsistencyCheck(effects.capsuleEffectsComp.methodEffects);
            for (SystemGraphs.Node n :
                     tree.sym.graphs.forwardConnectionEdges.keySet()) {
                cc.checkConsistency(tree.sym.graphs, n);
                }
        }*/
//    	tree.sym.graphs = graphsBuilder.buildGraphs(tree);
    	ListBuffer<JCStatement> decls = new ListBuffer<JCStatement>();
    	ListBuffer<JCStatement> inits = new ListBuffer<JCStatement>();
    	ListBuffer<JCStatement> assigns = new ListBuffer<JCStatement>();
    	ListBuffer<JCStatement> submits = new ListBuffer<JCStatement>();
    	ListBuffer<JCStatement> starts = new ListBuffer<JCStatement>();
    	ListBuffer<JCStatement> joins = new ListBuffer<JCStatement>();
    	Map<Name, Name> variables = new HashMap<Name, Name>();
    	Map<Name, Integer> modArrays = new HashMap<Name, Integer>();
    	
    	for(int i=0;i <tree.body.stats.length();i++){
    		JCStatement currentSystemStmt = tree.body.stats.get(i);
    		Tag systemStmtKind = currentSystemStmt.getTag();
    		if(systemStmtKind == VARDEF){
    			JCVariableDecl vdecl = (JCVariableDecl) currentSystemStmt;
    			Name vdeclTypeName = names.fromString(vdecl.vartype.toString());
    			if(syms.capsules.containsKey(vdeclTypeName))
    				org.paninij.comp.Attr.processCapsuleDef(tree, decls, inits, submits, starts, joins, variables, vdecl, make, names, syms, rs, env);
    			else{
    				if(vdecl.vartype.getTag()==CAPSULEARRAY){
    					org.paninij.comp.Attr.processCapsuleArray(tree, decls, assigns, submits, starts, joins, variables, modArrays, vdecl, make, names, syms, rs, env, log);
    				}
    				else{
    					if(vdecl.vartype.getTag()==TYPEIDENT || vdecl.vartype.toString().equals("String")){
    						vdecl.mods.flags |=FINAL;
    						decls.add(vdecl);
    					} else
    						log.error(vdecl.pos(), "only.primitive.types.or.strings.allowed");
    				}
    			}
    		} else if(systemStmtKind == EXEC){
    			JCExpressionStatement currentExprStmt = (JCExpressionStatement) currentSystemStmt;
    			if(currentExprStmt.expr.getTag()==APPLY){
    				processCapsuleWiring(currentExprStmt.expr, assigns, variables, make, names, syms, log);
    			}
    		}else if(systemStmtKind == FOREACHLOOP)
    			org.paninij.comp.Attr.processForEachLoop((JCEnhancedForLoop) currentSystemStmt, assigns, variables, make, names, syms, rs, env, log);
    		else if(systemStmtKind == MAAPPLY)
    			org.paninij.comp.Attr.processCapsuleArrayWiring((JCCapsuleArrayCall) currentSystemStmt, assigns, variables, modArrays, make, names, syms, rs, env, log);
    		else  			
    			throw new AssertionError("Invalid statement gone through the parser");
    	}
    	if(tree.hasTaskCapsule)
    		Attr.processSystemAnnotation(tree, inits, make, names, syms, rs, env, log, annotate);

    	List<JCStatement> mainStmts;
		mainStmts = decls.appendList(inits).appendList(assigns).appendList(starts).appendList(joins).appendList(submits).toList();
    	JCMethodDecl maindecl = org.paninij.comp.Attr.createMainMethod(tree.sym, tree.body, tree.params, mainStmts, make, names, syms);
    	tree.defs = tree.defs.append(maindecl);
    	
    	tree.switchToClass();
    	
    	memberEnter.memberEnter(maindecl, env);
        if (doGraphs) {
            //ListBuffer<Symbol> capsules = new ListBuffer<Symbol>();
            for (JCStatement v : decls) {
                if (v.getTag() == VARDEF) {
                    JCVariableDecl varDecl = (JCVariableDecl)v;
                    ClassSymbol c = syms.capsules.get(names.fromString(varDecl.vartype.toString()));
                    //System.out.println("indegree: "+ c.indegree + ", outdegree: "+ c.outdegree);
                    if (varDecl.vartype.toString().contains("[]")) {
//                    System.out.println("\n\n\nConsistency checker doesn't yet support capsule arrays. Exiting now.\n\n\n");
//                    System.exit(5);
                        //c = syms.capsules.get(names.fromString(varDecl.vartype.toString().substring(0, varDecl.vartype.toString().indexOf("["))));
                        
                    }
                    //if (!capsules.contains(c)) capsules.append(c);
                }
            }
            //tree.sym.capsules = capsules.toList();
        }
    }
   
    public static void visitProcDef(JCProcDecl tree, Log log){
    	Type restype = ((MethodType)tree.sym.type).restype;
    	if(restype.tsym.isCapsule||tree.sym.getReturnType().isPrimitive()||
    			tree.sym.getReturnType().toString().equals("java.lang.String"))
    	{
    		log.error("procedure.restype.illegal", tree.sym.getReturnType(), tree.sym);
    		System.exit(1);
    	}
    	tree.switchToMethod();
    }

	// Helper functions
    private static void processSystemAnnotation(JCSystemDecl tree, ListBuffer<JCStatement> stats, TreeMaker make, Names names, Symtab syms, 
    		Resolve rs, Env<AttrContext> env, Log log, Annotate annotate){
    	int numberOfPools = 1;
    	for(JCAnnotation annotation : tree.mods.annotations){
    		if(annotation.annotationType.toString().equals("Parallelism")){
    			if(annotation.args.isEmpty())
    				log.error(tree.pos(), "annotation.missing.default.value", annotation, "value");
    			else if (annotation.args.size()==1 && annotation.args.head.getTag()==ASSIGN){
    				if (annotate.enterAnnotation(annotation,
    						syms.annotationType, env).member(names.value).type == syms.intType)
    					numberOfPools = (Integer) annotate
    					.enterAnnotation(annotation,
    							syms.annotationType, env)
    							.member(names.value).getValue();
    			}
    		}
    	}
    	stats.prepend(make.Try(make.Block(0, List.<JCStatement> of(make
    			.Exec(make.Apply(List.<JCExpression> nil(), make
    					.Select(make.Ident(names
    							.fromString(PaniniConstants.PANINI_CAPSULE_TASK)),
    							names.fromString("init")), List
    							.<JCExpression> of(make.Literal(numberOfPools)))))),
    							List.<JCCatch> of(make.Catch(make.VarDef(
    									make.Modifiers(0), names.fromString("e"),
    									make.Ident(names.fromString("Exception")),
    									null), make.Block(0,
    											List.<JCStatement> nil()))), null));
    }

    public static JCMethodDecl createMainMethod(final ClassSymbol containingClass, final JCBlock methodBody, final List<JCVariableDecl> params, final List<JCStatement> mainStmts, 
    		TreeMaker make, Names names, Symtab syms) {
    	Type arrayType = new ArrayType(syms.stringType, syms.arrayClass);
    	MethodSymbol msym = new MethodSymbol(
    			PUBLIC|STATIC,
    			names.fromString("main"),
    			new MethodType(
    					List.<Type>of(arrayType),
    					syms.voidType,
    					List.<Type>nil(),
    					syms.methodClass
    					),
    					containingClass
    			);
    	JCMethodDecl maindecl = make.MethodDef(msym, methodBody);
    	JCVariableDecl mainArg = null; 
    	if(params.length() == 0) {
    		mainArg = make.Param( names.fromString("args"), arrayType, msym);
    		maindecl.params = List.<JCVariableDecl>of(mainArg);
    	} else maindecl.params = params;

    	maindecl.body.stats = mainStmts;
    	return maindecl;
    }

    private static void processCapsuleArrayWiring(JCCapsuleArrayCall mi,
    		ListBuffer<JCStatement> assigns, Map<Name, Name> variables,
    		Map<Name, Integer> modArrays, TreeMaker make, Names names, Symtab syms, 
    		Resolve rs, Env<AttrContext> env, Log log) {
    	if(!variables.containsKey(names
    			.fromString(mi.name.toString()))){
    		log.error(mi.pos(), "symbol.not.found");
    	}
    	CapsuleSymbol c = (CapsuleSymbol) rs
    			.findType(env, variables.get(names
    					.fromString(mi.name.toString())));
    	if(mi.index.getTag()!=Tag.LITERAL)
    		log.error(mi.index.pos(), "capsule.array.call.illegal.index");
    	JCLiteral ind = (JCLiteral)mi.index;
    	if((Integer)ind.value<0||(Integer)ind.value>=modArrays.get(names
    			.fromString(mi.name.toString())))
    	{
    		log.error(mi.index.pos(), "capsule.array.call.index.out.of.bound", ind.value, modArrays.get(names
    				.fromString(mi.name.toString())));
    	}
    	if (mi.arguments.length() != c.capsuleParameters.length()) {
    		log.error(mi.pos(), "arguments.of.wiring.mismatch");
    	} else {
    		for (int j = 0; j < mi.arguments.length(); j++) {
    			JCAssign newAssign = make
    					.at(mi.pos())
    					.Assign(make.Select
    							(make.TypeCast(make.Ident(variables.get(names
    									.fromString(mi.indexed.toString()))), make.Indexed
    									(mi.indexed, 
    											mi.index)),
    											c.capsuleParameters.get(j)
    											.getName()), mi.arguments.get(j));
    			JCExpressionStatement assignAssign = make
    					.Exec(newAssign);
    			assigns.append(assignAssign);
    		}
    	}
    }

    public static void processForEachLoop(JCEnhancedForLoop loop, ListBuffer<JCStatement> assigns, Map<Name, Name> variables, TreeMaker make, Names names, Symtab syms, 
    		Resolve rs, Env<AttrContext> env, Log log) {
    	CapsuleSymbol c = syms.capsules.get(names.fromString(loop.var.vartype.toString()));
    	if(c==null){
    		log.error(loop.pos(), "capsule.array.type.error", loop.var.vartype);
    	}
    	CapsuleSymbol d = syms.capsules.get(variables.get(names.fromString(loop.expr.toString())));
    	if(d==null)
    		log.error(loop.expr.pos(), "symbol.not.found");
    	variables.put(loop.var.name, names.fromString(d.name.toString()));
    	//					if(!types.isSameType(c.type, d.type)){
    	//						log.error(loop.var.pos(),"expected", d.type);
    	//					}// this won't work any more
    	// Type checking: find a way to check if type matches
    	ListBuffer<JCStatement> loopBody = new ListBuffer<JCStatement>();
    	JCVariableDecl vdecl = make.at(loop.pos).VarDef(make.Modifiers(0), 
    			loop.var.name,
    			make.Ident(d.name),
    			make.Indexed(loop.expr, make.Ident(names.fromString("index$"))));
    	loopBody.add(vdecl);

    	JCVariableDecl arraycache = make.at(loop.pos).VarDef(make.Modifiers(0), 
    			names.fromString("index$"), 
    			make.TypeIdent(INT), 
    			make.Literal(0));
    	JCBinary cond = make.at(loop.pos).Binary(LT, make.Ident(names.fromString("index$")),
    			make.Select(loop.expr, 
    					names.fromString("length")));
    	JCUnary unary = make.at(loop.pos).Unary(PREINC, make.Ident(names.fromString("index$")));
    	JCExpressionStatement step = 
    			make.at(loop.pos).Exec(unary);
    	if(loop.body.getTag() == Tag.BLOCK){
    		JCBlock jb = (JCBlock)loop.body;
    		for(JCStatement s : jb.stats){
    			if(s.getTag()!=EXEC){
    				log.error(s.pos(),"foreachloop.statement.error");
    			}else if(((JCExpressionStatement)s).expr.getTag()!=APPLY){
    				log.error(s.pos(),"foreachloop.statement.error");
    			}
    			JCMethodInvocation mi = (JCMethodInvocation)((JCExpressionStatement)s).expr;
    			loopBody.appendList(transWiring(mi,variables, make, names, syms, log));
    		}
    	}
    	else{
    		if(loop.body.getTag()!=EXEC){
    			log.error(loop.body.pos(),"foreachloop.statement.error");
    		}else if(((JCExpressionStatement)loop.body).expr.getTag()!=APPLY){
    			log.error(loop.body.pos(),"foreachloop.statement.error");
    		}
    		JCMethodInvocation mi = (JCMethodInvocation)((JCExpressionStatement)loop.body).expr;
    		loopBody.appendList(transWiring(mi,variables, make, names, syms, log));
    	}

    	JCForLoop floop = 
    			make.at(loop.pos).ForLoop(List.<JCStatement>of(arraycache), 
    					cond, 
    					List.of(step), 
    					make.Block(0, loopBody.toList()));
    	assigns.append(floop);
    }
    
    public static void processCapsuleWiring(final JCExpression wiring, final ListBuffer<JCStatement> assigns, final Map<Name, Name> variables, TreeMaker make, Names names, Symtab syms, Log log) {
    	JCMethodInvocation mi = (JCMethodInvocation) wiring;
    	try{
    		assigns.appendList(transWiring(mi, variables, make, names, syms, log));
    	}catch (NullPointerException e){
    		log.error(mi.pos(), "only.capsule.types.allowed");
    	}
    }

    private static List<JCStatement> transWiring(final JCMethodInvocation mi, final Map<Name, Name> variables, TreeMaker make, Names names, Symtab syms, Log log){
    	if(variables.get(names
    			.fromString(mi.meth.toString()))==null){
    		log.error(mi.pos(), "capsule.array.type.error", mi.meth);
    	}
    	CapsuleSymbol c = syms.capsules.get(variables.get(names
				.fromString(mi.meth.toString())));
    	ListBuffer<JCStatement> assigns = new ListBuffer<JCStatement>();

    	if (mi.args.length() != c.capsuleParameters.length()) {
    		log.error(mi.pos(), "arguments.of.wiring.mismatch");
    	} else {
    		for (int j = 0; j < mi.args.length(); j++) {
    			JCAssign newAssign = make
    					.at(mi.pos())
    					.Assign(make.Select(make.TypeCast(make.Ident(c), mi.meth),
    							c.capsuleParameters.get(j)
    							.getName()), mi.args.get(j));
    			JCExpressionStatement assignAssign = make
    					.Exec(newAssign);
    			assigns.append(assignAssign);
    		}
    	}
    	return assigns.toList();
    }

    public static void processCapsuleArray(JCSystemDecl tree,
    		ListBuffer<JCStatement> decls, ListBuffer<JCStatement> assigns,
    		ListBuffer<JCStatement> submits, ListBuffer<JCStatement> starts,
    		ListBuffer<JCStatement> joins, Map<Name, Name> variables,
    		Map<Name, Integer> modArrays, JCVariableDecl vdecl,
    		TreeMaker make, Names names, Symtab syms, 
    		Resolve rs, Env<AttrContext> env, Log log) {
    	JCCapsuleArray mat = (JCCapsuleArray)vdecl.vartype;
    	String initName = mat.elemtype.toString()+"$thread";
    	if((vdecl.mods.flags & Flags.TASK) !=0){
    		initName = mat.elemtype.toString()+"$task";
    		tree.hasTaskCapsule = true;
    	}
    	else if((vdecl.mods.flags & Flags.SERIAL) !=0)
    		initName = mat.elemtype.toString()+"$serial";
    	else if((vdecl.mods.flags & Flags.MONITOR) !=0)
    		initName = mat.elemtype.toString()+"$monitor";
    	CapsuleSymbol c = syms.capsules.get(names.fromString(initName));
    	if(c==null){
    		log.error(vdecl.pos(), "capsule.array.type.error", mat.elemtype);
    	}
    	JCNewArray s= make.NewArray(make.Ident(c.type.tsym), 
    			List.<JCExpression>of(make.Literal(mat.amount)), null);
    	JCVariableDecl newArray = 
    			make.VarDef(make.Modifiers(0), 
    					vdecl.name, make.TypeArray(make.Ident(c.type.tsym)), s);
    	decls.add(newArray);
    	ListBuffer<JCStatement> loopBody = new ListBuffer<JCStatement>();
    	JCVariableDecl arraycache = make.VarDef(make.Modifiers(0), 
    			names.fromString("index$"), 
    			make.TypeIdent(INT), 
    			make.Literal(0));
    	JCBinary cond = make.Binary(LT, make.Ident(names.fromString("index$")),
    			make.Select(make.Ident(vdecl.name), 
    					names.fromString("length")));
    	JCUnary unary = make.Unary(PREINC, make.Ident(names.fromString("index$")));
    	JCExpressionStatement step = 
    			make.Exec(unary);
    	JCNewClass newClass = make.NewClass(null, null, 
    			make.QualIdent(c.type.tsym), List.<JCExpression>nil(), null);
    	newClass.constructor = rs.resolveConstructor
    			(tree.pos(), env, c.type, List.<Type>nil(), null,false,false);
    	newClass.type = c.type;
    	loopBody.add(make.Exec(make.Assign(
    			make.Indexed(make.Ident(vdecl.name), make.Ident(names.fromString("index$"))), 
    			newClass)));
    	JCForLoop floop = 
    			make.ForLoop(List.<JCStatement>of(arraycache), 
    					cond, 
    					List.of(step), 
    					make.Block(0, loopBody.toList()));
    	assigns.append(floop);
    	if(c.definedRun){
    		for(int j = mat.amount-1; j>=0;j--){
    			if(tree.activeCapsuleCount==0)
    				starts.append(make.Exec(make.Apply(List.<JCExpression>nil(), 
    					make.Select(make.Indexed(make.Ident(vdecl.name), make.Literal(j)), names.fromString("run")), 
    					List.<JCExpression>nil())));
    			else{
    				starts.prepend(make.Exec(make.Apply(List.<JCExpression>nil(), 
    	    				make.Select(make.Indexed(make.Ident(vdecl.name), make.Literal(j)), names.fromString("start")), 
    	    				List.<JCExpression>nil())));
    				joins.prepend(make.Try(make.Block(0,List.<JCStatement>of(make.Exec(make.Apply(List.<JCExpression>nil(), 
        					make.Select(make.Indexed(make.Ident(vdecl.name), make.Literal(j)),
        							names.fromString("join")), List.<JCExpression>nil())))), 
        							List.<JCCatch>of(make.Catch(make.VarDef(make.Modifiers(0), 
        									names.fromString("e"), make.Ident(names.fromString("InterruptedException")), 
        									null), make.Block(0, List.<JCStatement>nil()))), null));
    			}
    			tree.activeCapsuleCount += mat.amount;
    		}
    	}
    	else{
    		for(int j = mat.amount-1; j>=0;j--){
    			starts.prepend(make.Exec(make.Apply(List.<JCExpression>nil(), 
    					make.Select(make.Indexed(make.Ident(vdecl.name), make.Literal(j)), names.fromString("start")), 
    					List.<JCExpression>nil())));
    		}
    		for(int j=0; j<mat.amount;j++){
    			submits.append(make.Exec(make.Apply(List.<JCExpression>nil(), 
    					make.Select(make.Indexed(make.Ident(vdecl.name), make.Literal(j)), 
    							names.fromString(PaniniConstants.PANINI_SHUTDOWN)), List.<JCExpression>nil())));
    		}
    	}
    	//					for(int j = 0; j<mat.amount; j++)
    	//						tree.defs = tree.defs.append(createOwnerInterface(mat.elemtype.toString()+"_"+vdecl.name.toString()+"_"+j));

    	variables.put(vdecl.name, c.name);
    	modArrays.put(vdecl.name, mat.amount);
    }

    public static void processCapsuleDef(JCSystemDecl tree,
    		ListBuffer<JCStatement> decls, ListBuffer<JCStatement> inits,
    		ListBuffer<JCStatement> submits, ListBuffer<JCStatement> starts,
    		ListBuffer<JCStatement> joins, Map<Name, Name> variables,
    		JCVariableDecl vdecl, TreeMaker make, Names names, Symtab syms, 
    		Resolve rs, Env<AttrContext> env) {
    	String initName = vdecl.vartype.toString()+"$thread";
    	if((vdecl.mods.flags & Flags.TASK) !=0){
    		initName = vdecl.vartype.toString()+"$task";
    		tree.hasTaskCapsule = true;
    	}
    	else if((vdecl.mods.flags & Flags.SERIAL) !=0)
    		initName = vdecl.vartype.toString()+"$serial";
    	else if((vdecl.mods.flags & Flags.MONITOR) !=0)
    		initName = vdecl.vartype.toString()+"$monitor";
    	CapsuleSymbol c = syms.capsules.get(names.fromString((initName)));
    	decls.add(vdecl);
    	JCNewClass newClass = make.at(vdecl.pos()).NewClass(null, null, 
    			make.Ident(c.type.tsym), List.<JCExpression>nil(), null);
    	newClass.constructor = rs.resolveConstructor
    			(tree.pos(), env, c.type, List.<Type>nil(), null,false,false);
    	newClass.type = c.type;
    	JCAssign newAssign = make.at(vdecl.pos()).Assign(make.Ident(vdecl.name),
    			newClass);
    	newAssign.type = vdecl.type;
    	JCExpressionStatement nameAssign = make.at(vdecl.pos()).Exec(newAssign);
    	nameAssign.type = vdecl.type;
    	inits.append(nameAssign);
    	JCExpressionStatement startAssign = make.Exec(make.Apply(List.<JCExpression>nil(), 
    			make.Select(make.Ident(vdecl.name), names.fromString("start")), 
    			List.<JCExpression>nil()));
    	if(c.definedRun){
    		if(tree.activeCapsuleCount==0)
	    		starts.append(make.Exec(make.Apply(List.<JCExpression>nil(), 
	    				make.Select(make.Ident(vdecl.name), names.fromString("run")), 
	    				List.<JCExpression>nil())));
    		else{
    			starts.prepend(startAssign);
    			joins.append(make.Try(make.Block(0,List.<JCStatement>of(make.Exec(make.Apply(List.<JCExpression>nil(), 
        				make.Select(make.Ident(vdecl.name), 
        						names.fromString("join")), List.<JCExpression>nil())))), 
        						List.<JCCatch>of(make.Catch(make.VarDef(make.Modifiers(0), 
        								names.fromString("e"), make.Ident(names.fromString("InterruptedException")), 
        								null), make.Block(0, List.<JCStatement>nil()))), null));
    		}
    		tree.activeCapsuleCount++;
    	}
    	else{
    		starts.prepend(startAssign);
    		submits.append(make.Exec(make.Apply(List.<JCExpression>nil(), 
    				make.Select(make.Ident(vdecl.name), 
    						names.fromString(PaniniConstants.PANINI_SHUTDOWN)), List.<JCExpression>nil())));
    	}
//    	JCClassDecl ownerIface = createOwnerInterface(
//    			vdecl.vartype.toString()+"_"+vdecl.name.toString(), make, names);
//    	enter.classEnter(ownerIface, env);
//    	tree.defs = tree.defs.append(ownerIface);

    	variables.put(vdecl.name, c.name);
    }

    public static JCClassDecl createOwnerInterface(final String interfaceName, 
    		TreeMaker make, Names names) {
    	JCClassDecl typeInterface = 
    			make.ClassDef(
    					make.Modifiers(PUBLIC|INTERFACE|SYNTHETIC), 
    					names.fromString(interfaceName), 
    					List.<JCTypeParameter>nil(), null, 
    					List.<JCExpression>nil(), 
    					List.<JCTree>nil());
    	return typeInterface;
    }
}
