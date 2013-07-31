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

import static com.sun.tools.javac.code.Flags.ACTIVE;
import static com.sun.tools.javac.code.Flags.FINAL;
import static com.sun.tools.javac.code.Flags.MONITOR;
import static com.sun.tools.javac.code.Flags.PUBLIC;
import static com.sun.tools.javac.code.Flags.SERIAL;
import static com.sun.tools.javac.code.Flags.STATIC;
import static com.sun.tools.javac.code.Flags.TASK;
import static com.sun.tools.javac.code.TypeTags.INT;
import static com.sun.tools.javac.tree.JCTree.Tag.ASSIGN;
import static com.sun.tools.javac.tree.JCTree.Tag.LT;
import static com.sun.tools.javac.tree.JCTree.Tag.PREINC;

import java.util.Map;
import java.util.Set;

import org.paninij.analysis.ASTCFGBuilder;
import org.paninij.consistency.ConsistencyUtil;
import org.paninij.consistency.ConsistencyUtil.SEQ_CONST_ALG;
import org.paninij.consistency.SeqConstCheckAlgorithm;
import org.paninij.systemgraph.SystemGraph;
import org.paninij.systemgraph.SystemGraph.Node;
import org.paninij.systemgraph.SystemGraphBuilder;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.CapsuleProcedure;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Annotate;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCapsuleDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCDesignBlock;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import org.paninij.util.PaniniConstants;

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
	public final Check pck;

    final ConsistencyUtil.SEQ_CONST_ALG seqConstAlg;

    protected static final Context.Key<Attr> attrKey =
            new Context.Key<Attr>();

    public static Attr instance(Context context) {
        Attr instance = context.get(attrKey);
        if (instance == null)
            instance = new Attr(context);
        return instance;
    }

	/**
	 * Whether or not capsule state access should be reported as an error.
	 * Used to the keep errors from being reported once a wiring block is
	 * converted to actual wiring statements.
	 *<p>
	 * Any logic which toggles this off must ensure it's state is restored
	 * after the specific case for turning it off has finished. e.g.
	 * <pre>
	 * boolean prevCheckCapState = checkCapStateAcc;
	 * try {
	 *     checkCapStateAcc = false;
	 *     ...rest of checks...
	 * } finally {
	 *     checkCapStateAcc = prevCheckCapState;
	 * }
	 * </pre>
	 */
	public boolean checkCapStateAcc = true;

    protected Attr(Context context) {
        super(TreeMaker.instance(context),
                com.sun.tools.javac.util.Names.instance(context),
                com.sun.tools.javac.code.Types.instance(context),
                com.sun.tools.javac.comp.Enter.instance(context),
                com.sun.tools.javac.comp.MemberEnter.instance(context),
                com.sun.tools.javac.code.Symtab.instance(context));
        context.put(attrKey, this);

        this.log = com.sun.tools.javac.util.Log.instance(context);
        this.annotate = Annotate.instance(context);
        this.annotationProcessor = new AnnotationProcessor(names, make, log);
        this.systemGraphBuilder = new SystemGraphBuilder(syms, names, log);
        this.pck = Check.instance(context);

        this.seqConstAlg = SEQ_CONST_ALG.instance(context);
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
//		System.out.println(tree);
//		System.out.println(tree.restype);
//		System.out.println(tree.restype.type);

		if ((tree.sym.owner.flags() & Flags.CAPSULE) != 0) {
			CapsuleProcedure cp = new CapsuleProcedure((ClassSymbol) tree.sym.owner,
					tree.name, tree.sym.params);
			((ClassSymbol) tree.sym.owner).capsule_info.procedures.put(tree.sym, cp);
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
	    tree.sym.capsule_info.connectedCapsules =
	            tree.sym.capsule_info.connectedCapsules.appendList(tree.params);

		if (tree.needsDefaultRun){
			List<JCClassDecl> wrapperClasses = generateClassWrappers(tree, env, rs);
			enter.classEnter(wrapperClasses, env.outer);
//			        	System.out.println(wrapperClasses);
			attr.attribClassBody(env, tree.sym);
			if((tree.sym.flags_field & TASK) !=0)
				tree.computeMethod.body = generateTaskCapsuleComputeMethodBody(tree);
			else
				tree.computeMethod.body = generateThreadCapsuleComputeMethodBody(tree);
			attr.attribStat(tree.computeMethod, env);
		}
		else {
		    attr.attribClassBody(env, tree.sym);
		    if (tree.computeMethod != null) {
				tree.computeMethod.body.stats = tree.computeMethod.body.stats
						.prepend(make.Exec(make.Apply(
								List.<JCExpression> nil(),
								make.Ident(names.panini.PaniniCapsuleInit),
								List.<JCExpression> nil())));
				if ((tree.sym.flags_field & ACTIVE) != 0) {
				    //Wire the system
				    tree.computeMethod.body.stats = tree.computeMethod.body.stats
	                        .prepend(make.Exec(createSimpleMethodCall(names.panini.InternalCapsuleWiring)));
					// Reference count disconnect()
					ListBuffer<JCStatement> blockStats = new ListBuffer<JCStatement>();
					blockStats = createCapsuleMemberDisconnects(
					                tree.sym.capsule_info.connectedCapsules);
					ListBuffer<JCStatement> body = new ListBuffer<JCStatement>();
					body.add(make.Try(
							make.Block(0, tree.computeMethod.body.stats),
							List.<JCCatch> nil(), body(blockStats)));
					tree.computeMethod.body.stats = body.toList();
				}
				attr.attribStat(tree.computeMethod, env);
			}
			if ((tree.sym.flags_field & SERIAL) != 0 || (tree.sym.flags_field & MONITOR) != 0) {
				// For serial capsule version
				ListBuffer<JCStatement> blockStats = new ListBuffer<JCStatement>();
				blockStats = createCapsuleMemberDisconnects(
				                tree.sym.capsule_info.connectedCapsules);
				ListBuffer<JCStatement> methodStats = new ListBuffer<JCStatement>();
				methodStats.append(make.Exec(make.Unary(JCTree.Tag.POSTDEC, make.Ident(names
						.fromString(PaniniConstants.PANINI_REF_COUNT)))));
			
				if (blockStats.size() > 0)
					methodStats.append(make.If(make.Binary(JCTree.Tag.EQ, make.Ident(names
							.fromString(PaniniConstants.PANINI_REF_COUNT)), make.Literal(TypeTags.INT, Integer.valueOf(0))), 
							make.Block(0, blockStats.toList()), null));
				
				JCBlock body = make.Block(0, methodStats.toList());
				
				JCMethodDecl disconnectMeth = null;
				MethodSymbol msym = null;
				msym = new MethodSymbol(PUBLIC | FINAL | Flags.SYNCHRONIZED,
						names.panini.PaniniDisconnect,
						new MethodType(List.<Type> nil(), syms.voidType,
								List.<Type> nil(), syms.methodClass), tree.sym);
				disconnectMeth = make.MethodDef(
						make.Modifiers(PUBLIC | FINAL | Flags.SYNCHRONIZED),
						names.panini.PaniniDisconnect,
						make.TypeIdent(TypeTags.VOID),
						List.<JCTypeParameter> nil(),
						List.<JCVariableDecl> nil(), List.<JCExpression> nil(),
						body, null);
				disconnectMeth.sym = msym;
				tree.defs = tree.defs.append(disconnectMeth);
				attr.attribStat(disconnectMeth, env);
			}
		}

		for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail){
			JCTree def = l.head;
			if(def.getTag() == Tag.METHODDEF){
				JCMethodDecl mdecl = (JCMethodDecl)def;
				for (List<JCVariableDecl> p = mdecl.params; p.nonEmpty(); p = p.tail){
					JCVariableDecl param = p.head;
					if((param.type.tsym.flags_field & Flags.CAPSULE) != 0 &&!mdecl.name.toString().contains("$Original")){
						log.error("procedure.argument.illegal", param, mdecl.name.toString(), tree.sym);
					}
				}
			}else if(def.getTag() == Tag.VARDEF){
				JCVariableDecl vdecl = (JCVariableDecl)def;
				if((vdecl.type.tsym.flags_field & Flags.CAPSULE) != 0)
					vdecl.mods.flags |= FINAL;
			}
		}

		pck.checkStateInit(tree.sym, env);
	}
	
	private ListBuffer<JCStatement> createCapsuleMemberDisconnects(
			List<JCVariableDecl> params) {
		ListBuffer<JCStatement> blockStats = new ListBuffer<JCStatement>();
		for (JCVariableDecl jcVariableDecl : params) {
			if (jcVariableDecl.vartype.type.tsym.isCapsule()) {
				JCStatement stmt = make
						.Exec(make.Apply(
								List.<JCExpression> nil(),
								make.Select(
										make.TypeCast(
												make.Ident(names
														.fromString(PaniniConstants.PANINI_QUEUE)),
												make.Ident(jcVariableDecl.name)),
										names.panini.PaniniDisconnect),
								List.<JCExpression> nil()));

				blockStats.append(stmt);
			} else if (jcVariableDecl.vartype.type.tsym.name.toString().equalsIgnoreCase("Array")) {
				if (((ArrayType)jcVariableDecl.vartype.type).elemtype.tsym.isCapsule()) {
					ListBuffer<JCStatement> loopBody = new ListBuffer<JCStatement>();
			        JCVariableDecl arraycache = make.VarDef(make.Modifiers(0),
			                names.fromString("index$"),
			                make.TypeIdent(INT),
			                make.Literal(0));
			        JCBinary cond = make.Binary(LT, make.Ident(names.fromString("index$")),
			                make.Select(make.Ident(jcVariableDecl.name),
			                        names.fromString("length")));
			        JCUnary unary = make.Unary(PREINC, make.Ident(names.fromString("index$")));
			        JCExpressionStatement step =
			                make.Exec(unary);
			        loopBody.add(make
							.Exec(make.Apply(
									List.<JCExpression> nil(),
									make.Select(
											make.TypeCast(
													make.Ident(names
															.fromString(PaniniConstants.PANINI_QUEUE)),
															make.Indexed(make.Ident(jcVariableDecl.name), 
																	make.Ident(names.fromString("index$")))),
																	names.panini.PaniniDisconnect),
									List.<JCExpression> nil())));
			        JCForLoop floop =
			                make.ForLoop(List.<JCStatement>of(arraycache),
			                        cond,
			                        List.of(step),
			                        make.Block(0, loopBody.toList()));
			        blockStats.append(floop);
				}
			}
		}
		return blockStats;
	}

	private void initRefCount(Map<Name, Name> variables,
			Map<Name, JCFieldAccess> refCountStats,
			ListBuffer<JCStatement> assigns, SystemGraph sysGraph,
			Env<AttrContext> env) {
		Set<Name> vars = sysGraph.nodes.keySet();
		final Name _this = names._this;
		final Name paniniRCField = names.panini.PaniniRefCountField;
		for (Name vdeclName : vars) {
		    // Reference count update
			int refCount = 0;
			refCount = sysGraph.nodes.get(vdeclName).indegree;

			JCFieldAccess accessStat = null;
			if (refCountStats.containsKey(vdeclName)) {
				accessStat = refCountStats.get(vdeclName);
			} else if (variables.containsKey(vdeclName)) {
				Name capsule = variables.get(vdeclName);
				accessStat = make.Select(
						make.TypeCast(make.Ident(capsule),
								make.Ident(vdeclName)), paniniRCField);
			} else if (_this.equals(vdeclName)) {
			    accessStat = make.Select(make.Ident(_this), paniniRCField);
		        env.enclClass.sym.capsule_info.refCount = refCount;
		    }
			if (accessStat == null)
				continue;
			JCAssign refCountAssign = make.Assign(accessStat, intlit(refCount));
			JCExpressionStatement refCountAssignStmt = make
					.Exec(refCountAssign);
			assigns.append(refCountAssignStmt);
		}
	}

	public final void visitSystemDef(JCDesignBlock tree, Resolve rs,
			com.sun.tools.javac.comp.Attr jAttr, // Javac Attributer.
			Env<AttrContext> env, boolean doGraphs){

	    tree.sym.flags_field= pck.checkFlags(tree, tree.sym.flags(), tree.sym);

	    //Use the scope of the out capsule, not the current system decl.
        Scope scope = enterSystemScope(env);
        moveWiringDecls(tree, scope);

	    ListBuffer<JCStatement> decls;
        ListBuffer<JCStatement> inits;
        ListBuffer<JCStatement> assigns;
        ListBuffer<JCStatement> starts;
        ListBuffer<JCStatement> joins;

        SystemGraph sysGraph;

        DesignDeclRewriter interp = new DesignDeclRewriter(make, log);
        JCDesignBlock rewritenTree = interp.rewrite(tree);

        CapsuleMainTransformer mt = new CapsuleMainTransformer(syms, names, types, log,
                rs, env, make, systemGraphBuilder);
        rewritenTree = mt.translate(rewritenTree);

        //pull data structures back out for reference here.
        decls = mt.decls;
        inits = mt.inits;
        assigns = mt.assigns;
        starts = mt.starts;
        joins = mt.joins;
        sysGraph = mt.sysGraph;

        if(rewritenTree.hasTaskCapsule)
			processSystemAnnotation(rewritenTree, inits, env);

        initRefCount(mt.variables, mt.refCountStats, assigns, sysGraph, env);
        
        //attribute the new statement.
        ListBuffer<JCStatement> toAttr = new ListBuffer<JCTree.JCStatement>();
        toAttr.addAll(decls);
        toAttr.addAll(inits);
        toAttr.addAll(assigns);
        toAttr.addAll(starts);
        toAttr.addAll(joins);
        final boolean prevCheckCapState = checkCapStateAcc;
        try {
            checkCapStateAcc = false;
            for (List<JCStatement> l = toAttr.toList(); l.nonEmpty(); l = l.tail) {
                jAttr.attribStat(l.head, env);
            }
        } finally {
            checkCapStateAcc = prevCheckCapState;
        }
		
		List<JCStatement> mainStmts;
		mainStmts = decls.appendList(inits).appendList(assigns).appendList(starts).toList();

		//TODO-XX: Still need to create a main method somewhere.
		//JCMethodDecl maindecl = createMainMethod(rewritenTree.sym.owner, rewritenTree.body, rewritenTree.params, mainStmts);


		systemGraphBuilder.completeEdges(sysGraph, annotationProcessor, env, rs);

		// Sequential consistency detection
		SeqConstCheckAlgorithm sca = 
		    ConsistencyUtil.createChecker(seqConstAlg, sysGraph, log);
		sca.potentialPathCheck();

		//replace the systemDef/wiring block with the new body.
		tree.body.stats = mainStmts;
	}

	public final void visitProcDef(JCProcDecl tree){
		Type restype = ((MethodType)tree.sym.type).restype;
		if((restype.tsym.flags_field & Flags.CAPSULE) == 1){
			log.error(tree.pos(), "procedure.restype.illegal.capsule");
		}
		tree.switchToMethod();
	}
	
	// Helper functions
	private void processSystemAnnotation(JCDesignBlock tree, ListBuffer<JCStatement> stats, Env<AttrContext> env){
		int numberOfPools = 1;
		for (List<JCAnnotation> l = tree.mods.annotations; l.nonEmpty(); l = l.tail) {
			JCAnnotation annotation = l.head;
			if (annotation.annotationType.toString().equals("Parallelism")) {
				if (annotation.args.isEmpty())
					log.error(tree.pos(), "annotation.missing.default.value",
							annotation, "value");
				else if (annotation.args.size() == 1
						&& annotation.args.head.getTag() == ASSIGN) {
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

    /**
     * @param tree
     * @return
     */
    private List<JCVariableDecl> extractWiringBlockDecls(JCDesignBlock tree) {
        class VarDeclCollector extends TreeScanner { //Helper visitor to collect var defs.
            final ListBuffer<JCVariableDecl> varDecls = new ListBuffer<JCVariableDecl>();
            @Override
            public final void visitVarDef(JCVariableDecl tree) {
                Type t = tree.vartype.type;
                if( t.tsym.isCapsule() ||
                        (types.isArray(t) && types.elemtype(t).tsym.isCapsule())) {
                    varDecls.add(tree);
                }
            }
        }
        VarDeclCollector vdc = new VarDeclCollector();
        tree.accept(vdc);
        return vdc.varDecls.toList();
    }

    /**
     * Move the capsule declarations in a wiring/design block out to the
     * 'field' scope and record the capsules decls in
     * {@link ClassSymbol#capsule_info#connectedCapsules}
     * @param wire
     * @param capScope
     */
    private void moveWiringDecls(JCDesignBlock wire, Scope capScope) {
        List<JCVariableDecl> capsuleDecls = extractWiringBlockDecls(wire);
        ClassSymbol capSym = wire.sym.ownerCapsule();
        capSym.capsule_info.connectedCapsules =
                capSym.capsule_info.connectedCapsules.appendList(capsuleDecls);

        // Enter the symbols into the capusle scope.
        // Allows the symbols to be visible for other procedures
        // and allows the symbols to be emitted as fields in the bytecode
        for(List<JCVariableDecl> l = capsuleDecls;  l.nonEmpty(); l = l.tail) {
            l.head.sym.owner = capScope.owner;
            capScope.enter(l.head.sym);
        }

        //TODO: Generics are being annoying. Find a way to not copy the list.
        ListBuffer<JCTree> capFields = new ListBuffer<JCTree>();
        for(List<JCVariableDecl> l = capsuleDecls; l.nonEmpty(); l = l.tail) {
            capFields.add(l.head);
            // Mark as private. Do not mark synthetic. Will cause other
            // name resolution to fail.
            l.head.sym.flags_field |= Flags.PRIVATE;
            //Update the AST Modifiers for pretty printing.
            l.head.mods = make.Modifiers(l.head.sym.flags_field);
        }

        // Copy the capsules over to the tree defs so they show up with
        // print-flat flag.
        JCCapsuleDecl tree = (JCCapsuleDecl)wire.sym.owner.tree;
        tree.defs = tree.defs.prependList(capFields.toList());
    }

    /** Get back to the 'class' level members scope from the current environment.
     * Fails if a class symbol cannot be found.
     * @param env
     */
    protected Scope enterSystemScope(Env<AttrContext> env) {
        while(env != null && !env.tree.hasTag(JCTree.Tag.CAPSULEDEF)) {
            env = env.next;
        }

        if(env != null) {
            return ((JCClassDecl)env.tree).sym.members_field;
        }
        Assert.error();
        return null;
    }
}
