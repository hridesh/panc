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
 * Contributor(s): Hridesh Rajan, Eric Lin, Sean L. Mooney
 */

package org.paninij.comp;


import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.code.TypeTags.*;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.paninij.analysis.ASTCFGBuilder;
import org.paninij.consistency.*;
import static org.paninij.consistency.ConsistencyUtil.SEQ_CONST_ALG;

import com.sun.tools.javac.code.CapsuleProcedure;
import com.sun.tools.javac.code.Attribute;
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
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.PaniniConstants;
import org.paninij.systemgraph.*;

/***
 * Panini-specific context-dependent analysis. All public functions in 
 * this class, are called from com.sun.tools.javac.comp.Attr to separate 
 * out Panini code. So, visitX in this class is called from method visitX 
 * in the class com.sun.tools.javac.comp.Attr.
 * 
 * @author hridesh
 *
 */
public final class Attr extends CapsuleInternal {
	Log log;
	Annotate annotate;
	AnnotationProcessor annotationProcessor;
	SystemGraphBuilder systemGraphBuilder;

	public Attr(TreeMaker make, Names names, Enter enter,
			MemberEnter memberEnter, Symtab syms, Log log,  
			Annotate annotate) {
		super(make, names, enter, memberEnter, syms);
		this.log = log;
		this.annotate = annotate;
		this.annotationProcessor = new AnnotationProcessor(names, make, log);
		this.systemGraphBuilder = new SystemGraphBuilder(syms, names, log);
	}

	public void visitTopLevel(JCCompilationUnit tree) { /* SKIPPED */ }
	public void visitImport(JCImport tree) { /* SKIPPED */ }
	public void visitLetExpr(LetExpr tree) { /* SKIPPED */ }
	public void visitAnnotation(JCAnnotation tree) { /* SKIPPED */ }
	public void visitModifiers(JCModifiers tree) { /* SKIPPED */ }
	public void visitErroneous(JCErroneous tree) { /* SKIPPED */ }
	public void visitTypeIdent(JCPrimitiveTypeTree tree) { /* SKIPPED */ }
	public void visitTypeApply(JCTypeApply tree) { /* SKIPPED */ }
	public void visitTypeUnion(JCTypeUnion tree) { /* SKIPPED */ }
	public void visitTypeParameter(JCTypeParameter tree) { /* SKIPPED */ }
	public void visitWildcard(JCWildcard tree) { /* SKIPPED */ }
	public void visitTypeBoundKind(TypeBoundKind tree) { /* SKIPPED */ }
	public void visitIdent(JCIdent tree) {  /* SKIPPED */ }
	public void visitLiteral(JCLiteral tree) {  /* SKIPPED */ }
	public void visitTypeArray(JCArrayTypeTree tree) {  /* SKIPPED */ }
	public void visitSkip(JCSkip tree) {  /* SKIPPED */ }
	public final void visitClassDef(JCClassDecl tree) {  /* SKIPPED */ }
	public final void visitLabelled(JCLabeledStatement tree) {  /* SKIPPED */ }
	public final void visitAssert(JCAssert tree) {  /* SKIPPED */ }

	public final void preVisitMethodDef(JCMethodDecl tree,
			final com.sun.tools.javac.comp.Attr attr) {
		if (tree.sym.isProcedure) {
			try {
				((JCProcDecl) tree).switchToProc();
				tree.accept(attr);
			} catch (ClassCastException e) {
			}
		}
	}

	public final void postVisitMethodDef(JCMethodDecl tree, Env<AttrContext> env, Resolve rs) {
		if (tree.body != null) {
			tree.accept(new ASTCFGBuilder());
		}

		if (tree.sym.owner instanceof CapsuleSymbol) {
			////
//			EffectSet es = new EffectSet();
//			es.add(es.bottomEffect());
//			es.add(es.methodEffect(tree.sym));
//			annotationProcessor.setEffects(tree, es);
//			annotate.enterAnnotation(tree.mods.annotations.last(), Type.noType, env);
			//// to add effectset: move out of if clause and remove test set; change second argument of setEffects to actual effectSet
			CapsuleProcedure cp = new CapsuleProcedure((CapsuleSymbol) tree.sym.owner,
					tree.name, tree.sym.params);
			((CapsuleSymbol) tree.sym.owner).procedures.put(tree.sym, cp);
			if(tree.sym.effect!=null){
				annotationProcessor.setEffects(tree, tree.sym.effect);
				Attribute.Compound buf = annotate.enterAnnotation(tree.mods.annotations.last(), Type.noType, env);
				tree.sym.attributes_field = tree.sym.attributes_field.append(buf); 
			}
		}
	}
	
	public final void visitVarDef(JCVariableDecl tree) {  /* SKIPPED */ }
	public final void visitBlock(JCBlock tree) {  /* SKIPPED */ }
	public final void visitDoLoop(JCDoWhileLoop tree) {  /* SKIPPED */ }
	public final void visitWhileLoop(JCWhileLoop tree){  /* SKIPPED */ }
	public final void visitForLoop(JCForLoop tree){  /* SKIPPED */ }
	public final void visitForeachLoop(JCEnhancedForLoop tree) {  /* SKIPPED */ }
	public final void visitSwitch(JCSwitch tree) {  /* SKIPPED */ }
	public final void visitCase(JCCase tree) {  /* SKIPPED */ }
	public final void visitSynchronized(JCSynchronized tree) {  /* SKIPPED */ }
	public final void visitTry(JCTry tree) {  /* SKIPPED */ }
	public final void visitCatch(JCCatch tree) {  /* SKIPPED */ }
	public final void visitConditional(JCConditional tree) {  /* SKIPPED */ }
	public final void visitIf(JCIf tree) {  /* SKIPPED */ }
	public final void visitExec(JCExpressionStatement tree) {  /* SKIPPED */ }
	public final void visitBreak(JCBreak tree) {  /* SKIPPED */ }
	public final void visitContinue(JCContinue tree) {  /* SKIPPED */ }
	public final void visitReturn(JCReturn tree) {  /* SKIPPED */ }
	public final void visitThrow(JCThrow tree) {  /* SKIPPED */ }
	public final void visitApply(JCMethodInvocation tree) {  /* SKIPPED */ }
	public final void visitNewClass(JCNewClass tree) {  /* SKIPPED */ }
	public final void visitNewArray(JCNewArray tree) {  /* SKIPPED */ }
	public final void visitParens(JCParens tree) {  /* SKIPPED */ }
	public final void visitAssign(JCAssign tree) {  /* SKIPPED */ }
	public final void visitAssignop(JCAssignOp tree) {  /* SKIPPED */ }
	public final void visitUnary(JCUnary tree) {  /* SKIPPED */ }
	public final void visitBinary(JCBinary tree) {  /* SKIPPED */ }
	public void visitTypeCast(JCTypeCast tree) {  /* SKIPPED */ }
	public void visitTypeTest(JCInstanceOf tree) {  /* SKIPPED */ }
	public void visitIndexed(JCArrayAccess tree) {  /* SKIPPED */ }
	public void visitSelect(JCFieldAccess tree) {  /* SKIPPED */ }
	
	public final void visitCapsuleDef(final JCCapsuleDecl tree, final com.sun.tools.javac.comp.Attr attr, Env<AttrContext> env, Resolve rs){
		if (tree.needsDefaultRun){
			List<JCClassDecl> wrapperClasses = generateClassWrappers(tree, env, rs);
			enter.classEnter(wrapperClasses, env.outer);
//			        	System.out.println(wrapperClasses);
			attr.attribClassBody(env, tree.sym);
			if((tree.sym.flags_field & TASK) !=0)
				tree.computeMethod.body = generateTaskCapsuleComputeMethodBody(tree);
			else{
				tree.computeMethod.body = generateThreadCapsuleComputeMethodBody(tree);
				tree.computeMethod.body.stats = tree.computeMethod.body.stats
						.prepend(make.Exec(make.Apply(
								List.<JCExpression> nil(),
								make.Ident(names
										.fromString(PaniniConstants.PANINI_CAPSULE_INIT)),
								List.<JCExpression> nil()))); 
			}
		}
		else {
			attr.attribClassBody(env, tree.sym);
			if(tree.computeMethod!=null)
				tree.computeMethod.body.stats = tree.computeMethod.body.stats
						.prepend(make.Exec(make.Apply(
								List.<JCExpression> nil(),
								make.Ident(names
										.fromString(PaniniConstants.PANINI_CAPSULE_INIT)),
								List.<JCExpression> nil())));
		}
		for(JCTree def : tree.defs){
			if(def instanceof JCMethodDecl){
				for(JCVariableDecl param : ((JCMethodDecl)def).params){
					if(param.type.tsym instanceof CapsuleSymbol&&!((JCMethodDecl)def).name.toString().contains("$Original")){
						log.error("procedure.argument.illegal", param, ((JCMethodDecl)def).name.toString(), tree.sym);
					}
				}
			}else if(def.getTag() == Tag.VARDEF){
				if(((JCVariableDecl)def).type.tsym instanceof CapsuleSymbol)
					((JCVariableDecl)def).mods.flags |= FINAL;
			}
		}
		
		/*if (doGraphs)
            effects.computeEffects(tree);*/
	}

	public final void visitSystemDef(final JCSystemDecl tree, Resolve rs, Env<AttrContext> env, boolean doGraphs, SEQ_CONST_ALG seqConstAlg){
		/*if (doGraphs) {
          ((Symbol.SystemSymbol)tree.sym).graphs = graphsBuilder.buildGraphs(tree);
            effects.substituteProcEffects(tree);
            ConsistencyCheck cc = 
                new ConsistencyCheck();
            for (SystemGraphs.Node n :
            	((Symbol.SystemSymbol)tree.sym).graphs.forwardConnectionEdges.keySet()) {
                Iterator<Symbol> iter = n.sym.members().getElements().iterator();
                while(iter.hasNext()){
                	Symbol s = iter.next();
                	if(s instanceof MethodSymbol){
                		MethodSymbol ms = (MethodSymbol)s;
                		if(ms.attributes_field.size()!=0){
                			for(Attribute.Compound compound : ms.attributes_field){
                				if(compound.type.tsym.getQualifiedName().toString().contains("Effects")){
                					ms.effects = annotationProcessor.translateEffectAnnotations(ms, compound, env, rs);
                				}
                			}
                		}
                	}
                }
                cc.checkConsistency(((Symbol.SystemSymbol)tree.sym).graphs, n);
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

		SystemGraph sysGraph = systemGraphBuilder.createSystemGraph();
		Set<Name> capsules = new HashSet<Name>();

		for(JCStatement currentSystemStmt : tree.body.stats){
			Tag systemStmtKind = currentSystemStmt.getTag();
			if(systemStmtKind == VARDEF){
				JCVariableDecl vdecl = (JCVariableDecl) currentSystemStmt;
				Name vdeclTypeName = names.fromString(vdecl.vartype.toString());
				if (syms.capsules.containsKey(vdeclTypeName)) 
					processCapsuleDef(tree, decls, inits, submits, starts,
							joins, variables, capsules, vdecl, rs, env, sysGraph);
				else {
					if (vdecl.vartype.getTag() == CAPSULEARRAY) 
						processCapsuleArray(tree, decls, assigns, submits,
								starts, joins, variables, capsules, modArrays, vdecl, rs,
								env, sysGraph);
					else {
						if (vdecl.vartype.getTag() == TYPEIDENT
								|| vdecl.vartype.toString().equals("String")) {
							vdecl.mods.flags |= FINAL;
							decls.add(vdecl);
						} else
							log.error(vdecl.pos(),
									"only.primitive.types.or.strings.allowed");
					}
				}
			} else if(systemStmtKind == EXEC){
				JCExpressionStatement currentExprStmt = (JCExpressionStatement) currentSystemStmt;
				if(currentExprStmt.expr.getTag()==APPLY){
					processCapsuleWiring(currentExprStmt.expr, assigns, variables, capsules, sysGraph);
				}
			}else if(systemStmtKind == FOREACHLOOP)
				processForEachLoop((JCEnhancedForLoop) currentSystemStmt, assigns, variables, capsules, sysGraph);
			else if(systemStmtKind == MAAPPLY)
				processCapsuleArrayWiring((JCCapsuleArrayCall) currentSystemStmt, assigns, variables, capsules, modArrays, rs, env, sysGraph);
			else  			
				throw new AssertionError("Invalid statement gone through the parser");
		}
		if(!capsules.isEmpty()){
			for(Name n : capsules){
				log.error("capsule.instance.not.initialized", n);
			}
		}
		if(tree.hasTaskCapsule)
			processSystemAnnotation(tree, inits, env);

		List<JCStatement> mainStmts;
		mainStmts = decls.appendList(inits).appendList(assigns).appendList(starts).appendList(joins).appendList(submits).toList();
		JCMethodDecl maindecl = createMainMethod(tree.sym, tree.body, tree.params, mainStmts);
		tree.defs = tree.defs.append(maindecl);

		systemGraphBuilder.completeEdges(sysGraph, annotationProcessor, env, rs);

		// Sequential consistency detection
		SeqConstCheckAlgorithm sca = 
		    ConsistencyUtil.createChecker(seqConstAlg, sysGraph, log);
		sca.potentialPathCheck();

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

	public final void visitProcDef(JCProcDecl tree){
		Type restype = ((MethodType)tree.sym.type).restype;
		if(restype.tsym instanceof CapsuleSymbol){
			log.error(tree.pos(), "procedure.restype.illegal.capsule");
		}
//		if(!(tree.sym.owner.toString().contains("$serial")||tree.sym.owner.toString().contains("$monitor"))){
//		if(tree.sym.getReturnType().isPrimitive()||
//				tree.sym.getReturnType().toString().equals("java.lang.String"))
//			{
//				switch (restype.tag){
//				case LONG:
//					tree.restype = make.Ident(names.fromString("Long"));
//					((MethodType)tree.sym.type).restype = syms.classType;
//				}
//			}
//		}
		tree.switchToMethod();
	}
	
	// Helper functions
	private void processSystemAnnotation(JCSystemDecl tree, ListBuffer<JCStatement> stats, Env<AttrContext> env){
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
								names.fromString(PaniniConstants.PANINI_INIT)), List
								.<JCExpression> of(make.Literal(numberOfPools)))))),
								List.<JCCatch> of(make.Catch(make.VarDef(
										make.Modifiers(0), names.fromString("e"),
										make.Ident(names.fromString("Exception")),
										null), make.Block(0,
												List.<JCStatement> nil()))), null));
	}

	private JCMethodDecl createMainMethod(final ClassSymbol containingClass, final JCBlock methodBody, final List<JCVariableDecl> params, final List<JCStatement> mainStmts) {
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

	private void processCapsuleArrayWiring(JCCapsuleArrayCall mi,
			ListBuffer<JCStatement> assigns, Map<Name, Name> variables,
			Set<Name> capsules, Map<Name, Integer> modArrays, Resolve rs, Env<AttrContext> env, SystemGraph sysGraph) {
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
				if(c.capsuleParameters.get(j).vartype.getTag()== Tag.TYPEARRAY){
					if(syms.capsules.containsKey(names
							.fromString(((JCArrayTypeTree)c.capsuleParameters.get(j).vartype).elemtype
									.toString())))
						systemGraphBuilder.addConnectionsOneToMany(sysGraph,
								names.fromString(mi.indexed.toString()+"["+mi.index+"]"),
								c.capsuleParameters.get(j).getName(),
								names.fromString(mi.arguments.get(j).toString()));
				}
				if (syms.capsules.containsKey(names
						.fromString(c.capsuleParameters.get(j).vartype
								.toString()))) {
					systemGraphBuilder.addConnection(sysGraph,
							names.fromString(mi.indexed.toString()+"["+mi.index+"]"),
							c.capsuleParameters.get(j).getName(),
							names.fromString(mi.arguments.get(j).toString()));
				}
			}
			capsules.remove(mi.name);
		}
	}

	private void processForEachLoop(JCEnhancedForLoop loop, ListBuffer<JCStatement> assigns, Map<Name, Name> variables, Set<Name> capsules, SystemGraph sysGraph) {
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
				loopBody.appendList(transWiring(mi,variables, capsules, sysGraph, true));
				if(loop.var.name.toString().equals(mi.meth.toString()))
					capsules.remove(names.fromString(loop.expr.toString()));
				if (mi.args.length() != c.capsuleParameters.length()) {
					log.error(mi.pos(), "arguments.of.wiring.mismatch");
				} else {
					for(int j=0;j<mi.args.length();j++){
						if(c.capsuleParameters.get(j).vartype.getTag()== Tag.TYPEARRAY){
							if(syms.capsules.containsKey(names
									.fromString(((JCArrayTypeTree)c.capsuleParameters.get(j).vartype).elemtype
											.toString())))
								systemGraphBuilder.addConnectionsManyToMany(sysGraph, 
										names.fromString(loop.expr.toString()), c.capsuleParameters.get(j).getName(), 
										names.fromString(mi.args.get(j).toString()));
						}
						if (syms.capsules.containsKey(names
								.fromString(c.capsuleParameters.get(j).vartype
										.toString()))) {
							systemGraphBuilder.addConnectionsManyToOne(sysGraph,
									names.fromString(loop.expr.toString()),
									c.capsuleParameters.get(j).getName(),
									names.fromString(mi.args.get(j).toString()));
						}
					}
				}
			}
		}
		else{
			if(loop.body.getTag()!=EXEC){
				log.error(loop.body.pos(),"foreachloop.statement.error");
			}else if(((JCExpressionStatement)loop.body).expr.getTag()!=APPLY){
				log.error(loop.body.pos(),"foreachloop.statement.error");
			}
			JCMethodInvocation mi = (JCMethodInvocation)((JCExpressionStatement)loop.body).expr;
			loopBody.appendList(transWiring(mi,variables, capsules, sysGraph, true));
			if(loop.var.name.toString().equals(mi.meth.toString()))
				capsules.remove(names.fromString(loop.expr.toString()));
			if (mi.args.length() != c.capsuleParameters.length()) {
				log.error(mi.pos(), "arguments.of.wiring.mismatch");
			} else {
				for(int j=0;j<mi.args.length();j++){
					if(c.capsuleParameters.get(j).vartype.getTag()== Tag.TYPEARRAY){
						if(syms.capsules.containsKey(names
								.fromString(((JCArrayTypeTree)c.capsuleParameters.get(j).vartype).elemtype
										.toString())))
							systemGraphBuilder.addConnectionsManyToMany(sysGraph, 
									names.fromString(loop.expr.toString()), c.capsuleParameters.get(j).getName(), 
									names.fromString(mi.args.get(j).toString()));
					}
					if (syms.capsules.containsKey(names
							.fromString(c.capsuleParameters.get(j).vartype
									.toString()))) {
						systemGraphBuilder.addConnectionsManyToOne(sysGraph,
								names.fromString(loop.expr.toString()),
								c.capsuleParameters.get(j).getName(),
								names.fromString(mi.args.get(j).toString()));
					}
				}
			}
		}

		JCForLoop floop = 
				make.at(loop.pos).ForLoop(List.<JCStatement>of(arraycache), 
						cond, 
						List.of(step), 
						make.Block(0, loopBody.toList()));
		assigns.append(floop);
	}

	private void processCapsuleWiring(final JCExpression wiring, final ListBuffer<JCStatement> assigns, final Map<Name, Name> variables, Set<Name> capsules, SystemGraph sysGraph) {
		JCMethodInvocation mi = (JCMethodInvocation) wiring;
		try{
			assigns.appendList(transWiring(mi, variables, capsules, sysGraph, false));
		}catch (NullPointerException e){
			log.error(mi.pos(), "only.capsule.types.allowed");
		}
	}

	private List<JCStatement> transWiring(final JCMethodInvocation mi, final Map<Name, Name> variables, Set<Name> capsules, SystemGraph sysGraph, boolean forEachLoop){
		if(variables.get(names
				.fromString(mi.meth.toString()))==null){
			log.error(mi.pos(), "capsule.type.error", mi.meth);
		}
		CapsuleSymbol c = syms.capsules.get(variables.get(names
				.fromString(mi.meth.toString())));
		ListBuffer<JCStatement> assigns = new ListBuffer<JCStatement>();

		if (mi.args.length() != c.capsuleParameters.length()) {
			log.error(mi.pos(), "arguments.of.wiring.mismatch");
		} else {
			for (int j = 0; j < mi.args.length(); j++) {
				String param = "";
				if(c.capsuleParameters.get(j).vartype.toString().contains("[")){
					param = c.capsuleParameters.get(j).vartype.toString().substring(0, c.capsuleParameters.get(j).vartype.toString().indexOf("["));
				}else
					param = c.capsuleParameters.get(j).vartype.toString();
				if(syms.capsules.containsKey(names.fromString(param))){//if its a capsule type
					if(mi.args.get(j).toString().equals("null")){
						log.error(mi.args.get(j).pos(), "capsule.null.declare");
					}else{
						String argument = "";
						if(mi.args.get(j).toString().contains("[")){
							argument = mi.args.get(j).toString().substring(0, mi.args.get(j).toString().indexOf("["));
						}else
							argument = mi.args.get(j).toString();
						if(mi.args.get(j).getTag()!=Tag.NEWARRAY)
							if(variables.get(names.fromString(argument))==null){
								log.error(mi.args.get(j).pos, "symbol.not.found");
							}
					}
				}
				JCAssign newAssign = make
						.at(mi.pos())
						.Assign(make.Select(make.TypeCast(make.Ident(c), mi.meth),
								c.capsuleParameters.get(j)
								.getName()), mi.args.get(j));
				JCExpressionStatement assignAssign = make
						.Exec(newAssign);
				assigns.append(assignAssign);
				if(!forEachLoop){
					if(c.capsuleParameters.get(j).vartype.getTag()== Tag.TYPEARRAY){
						if(mi.args.get(j).getTag()!=Tag.NEWARRAY)
							if(syms.capsules.containsKey(names
									.fromString(((JCArrayTypeTree)c.capsuleParameters.get(j).vartype).elemtype
											.toString())))
								systemGraphBuilder.addConnectionsOneToMany(sysGraph, 
										names.fromString(mi.meth.toString()), c.capsuleParameters.get(j).getName(), 
										names.fromString(mi.args.get(j).toString()));
					}
					if (syms.capsules.containsKey(names
							.fromString(c.capsuleParameters.get(j).vartype
									.toString()))) {
						systemGraphBuilder.addConnection(sysGraph,
								names.fromString(mi.meth.toString()),
								c.capsuleParameters.get(j).getName(),
								names.fromString(mi.args.get(j).toString()));
					}
				}
			}
			capsules.remove(names.fromString(mi.meth.toString()));
		}
		return assigns.toList();
	}

	private void processCapsuleArray(JCSystemDecl tree,
			ListBuffer<JCStatement> decls, ListBuffer<JCStatement> assigns,
			ListBuffer<JCStatement> submits, ListBuffer<JCStatement> starts,
			ListBuffer<JCStatement> joins, Map<Name, Name> variables,
			Set<Name> capsules, Map<Name, Integer> modArrays,
			JCVariableDecl vdecl, Resolve rs, Env<AttrContext> env, SystemGraph sysGraph) {
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
		systemGraphBuilder.addMultipleNodes(sysGraph, vdecl.name, mat.amount, c);
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
							make.Select(make.Indexed(make.Ident(vdecl.name), make.Literal(j)), names.fromString(PaniniConstants.PANINI_START)), 
							List.<JCExpression>nil())));
					joins.prepend(make.Try(make.Block(0,List.<JCStatement>of(make.Exec(make.Apply(List.<JCExpression>nil(), 
							make.Select(make.Indexed(make.Ident(vdecl.name), make.Literal(j)),
									names.fromString(PaniniConstants.PANINI_JOIN)), List.<JCExpression>nil())))), 
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
						make.Select(make.Indexed(make.Ident(vdecl.name), make.Literal(j)), names.fromString(PaniniConstants.PANINI_START)), 
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
		if(c.capsuleParameters.nonEmpty())
			capsules.add(vdecl.name);
		modArrays.put(vdecl.name, mat.amount);
	}

	private void processCapsuleDef(JCSystemDecl tree,
			ListBuffer<JCStatement> decls, ListBuffer<JCStatement> inits,
			ListBuffer<JCStatement> submits, ListBuffer<JCStatement> starts,
			ListBuffer<JCStatement> joins, Map<Name, Name> variables,
			Set<Name> capsules, JCVariableDecl vdecl, Resolve rs, Env<AttrContext> env, SystemGraph sysGraph) {
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
		if(c==null)
			log.error(vdecl.pos, "invalid.capsule.type");
		systemGraphBuilder.addSingleNode(sysGraph, vdecl.name, c);
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
				make.Select(make.Ident(vdecl.name), names.fromString(PaniniConstants.PANINI_START)), 
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
								names.fromString(PaniniConstants.PANINI_JOIN)), List.<JCExpression>nil())))), 
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
		if(c.capsuleParameters.nonEmpty())
			capsules.add(vdecl.name);
	}
}
