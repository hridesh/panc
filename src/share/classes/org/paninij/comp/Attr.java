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
import static com.sun.tools.javac.code.Flags.TASK;
import static com.sun.tools.javac.code.TypeTags.INT;
import static com.sun.tools.javac.tree.JCTree.Tag.ASSIGN;
import static com.sun.tools.javac.tree.JCTree.Tag.LT;
import static com.sun.tools.javac.tree.JCTree.Tag.PREINC;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.CapsuleProcedure;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Scope.Entry;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.comp.Annotate;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCapsuleDecl;
import com.sun.tools.javac.tree.JCTree.JCCapsuleLambda;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCStateDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCDesignBlock;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;

import org.paninij.analysis.ASTCFGBuilder;
import org.paninij.consistency.ConsistencyUtil;
import org.paninij.consistency.ConsistencyUtil.SEQ_CONST_ALG;
import org.paninij.consistency.SeqConstCheckAlgorithm;
import org.paninij.systemgraph.SystemGraph;
import org.paninij.systemgraph.SystemGraphBuilder;
import org.paninij.util.PaniniConstants;

/***
 * Panini-specific context-dependent analysis. All public functions in this
 * class, are called from com.sun.tools.javac.comp.Attr to separate out Panini
 * code. So, visitX in this class is called from method visitX in the class
 * com.sun.tools.javac.comp.Attr.
 * 
 * @author hridesh
 * 
 */
public final class Attr extends CapsuleInternal {
	Log log;
	Annotate annotate;
	AnnotationProcessor annotationProcessor;
	SystemGraphBuilder systemGraphBuilder;
	final com.sun.tools.javac.comp.Check jchk;
	public final Check pchk;
	public List<JCDesignBlock> designBlocks = List.<JCDesignBlock> nil();
	public final Map<Name, java.util.List<Name>> capsuleAliases = new HashMap<Name, java.util.List<Name>>();

	final ConsistencyUtil.SEQ_CONST_ALG seqConstAlg;

	protected static final Context.Key<Attr> attrKey = new Context.Key<Attr>();

	public static Attr instance(Context context) {
		Attr instance = context.get(attrKey);
		if (instance == null)
			instance = new Attr(context);
		return instance;
	}

	/**
	 * Whether or not capsule state access should be reported as an error. Used
	 * to the keep errors from being reported once a wiring block is converted
	 * to actual wiring statements.
	 * <p>
	 * Any logic which toggles this off must ensure it's state is restored after
	 * the specific case for turning it off has finished. e.g.
	 * 
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
		super(TreeMaker.instance(context), com.sun.tools.javac.util.Names
				.instance(context), com.sun.tools.javac.code.Types
				.instance(context), com.sun.tools.javac.comp.Enter
				.instance(context), com.sun.tools.javac.comp.MemberEnter
				.instance(context), com.sun.tools.javac.code.Symtab
				.instance(context), com.sun.tools.javac.comp.Resolve
				.instance(context));
		context.put(attrKey, this);

		this.log = com.sun.tools.javac.util.Log.instance(context);
		this.annotate = Annotate.instance(context);
		this.annotationProcessor = new AnnotationProcessor(names, make, log);
		this.systemGraphBuilder = new SystemGraphBuilder(syms, names, log);
		this.jchk = com.sun.tools.javac.comp.Check.instance(context);
		this.pchk = Check.instance(context);

		this.seqConstAlg = SEQ_CONST_ALG.instance(context);
	}

	public void visitTopLevel(JCCompilationUnit tree) { /* SKIPPED */
	}

	public void visitImport(JCImport tree) { /* SKIPPED */
	}

	public void visitLetExpr(LetExpr tree) { /* SKIPPED */
	}

	public void visitAnnotation(JCAnnotation tree) { /* SKIPPED */
	}

	public void visitModifiers(JCModifiers tree) { /* SKIPPED */
	}

	public void visitErroneous(JCErroneous tree) { /* SKIPPED */
	}

	public void visitTypeIdent(JCPrimitiveTypeTree tree) { /* SKIPPED */
	}

	public void visitTypeApply(JCTypeApply tree) { /* SKIPPED */
	}

	public void visitTypeUnion(JCTypeUnion tree) { /* SKIPPED */
	}

	public void visitTypeParameter(JCTypeParameter tree) { /* SKIPPED */
	}

	public void visitWildcard(JCWildcard tree) { /* SKIPPED */
	}

	public void visitTypeBoundKind(TypeBoundKind tree) { /* SKIPPED */
	}

	public void visitIdent(JCIdent tree) { /* SKIPPED */
	}

	public void visitLiteral(JCLiteral tree) { /* SKIPPED */
	}

	public void visitTypeArray(JCArrayTypeTree tree) { /* SKIPPED */
	}

	public void visitSkip(JCSkip tree) { /* SKIPPED */
	}

	public final void visitClassDef(JCClassDecl tree) { /* SKIPPED */
	}

	public final void visitLabelled(JCLabeledStatement tree) { /* SKIPPED */
	}

	public final void visitAssert(JCAssert tree) { /* SKIPPED */
	}

	public final void visitVarDef(JCVariableDecl tree) { /* SKIPPED */
	}

	public final void visitBlock(JCBlock tree) { /* SKIPPED */
	}

	public final void visitDoLoop(JCDoWhileLoop tree) { /* SKIPPED */
	}

	public final void visitWhileLoop(JCWhileLoop tree) { /* SKIPPED */
	}

	public final void visitForLoop(JCForLoop tree) { /* SKIPPED */
	}

	public final void visitForeachLoop(JCEnhancedForLoop tree) { /* SKIPPED */
	}

	public final void visitSwitch(JCSwitch tree) { /* SKIPPED */
	}

	public final void visitCase(JCCase tree) { /* SKIPPED */
	}

	public final void visitSynchronized(JCSynchronized tree) { /* SKIPPED */
	}

	public final void visitTry(JCTry tree) { /* SKIPPED */
	}

	public final void visitCatch(JCCatch tree) { /* SKIPPED */
	}

	public final void visitConditional(JCConditional tree) { /* SKIPPED */
	}

	public final void visitIf(JCIf tree) { /* SKIPPED */
	}

	public final void visitExec(JCExpressionStatement tree) { /* SKIPPED */
	}

	public final void visitBreak(JCBreak tree) { /* SKIPPED */
	}

	public final void visitContinue(JCContinue tree) { /* SKIPPED */
	}

	public final void visitReturn(JCReturn tree) { /* SKIPPED */
	}

	public final void visitThrow(JCThrow tree) { /* SKIPPED */
	}

	public final void visitApply(JCMethodInvocation tree) { /* SKIPPED */
	}

	public final void visitNewClass(JCNewClass tree) { /* SKIPPED */
	}

	public final void visitNewArray(JCNewArray tree) { /* SKIPPED */
	}

	public final void visitParens(JCParens tree) { /* SKIPPED */
	}

	public final void visitAssign(JCAssign tree) { /* SKIPPED */
	}

	public final void visitAssignop(JCAssignOp tree) { /* SKIPPED */
	}

	public final void visitUnary(JCUnary tree) { /* SKIPPED */
	}

	public final void visitBinary(JCBinary tree) { /* SKIPPED */
	}

	public void visitTypeCast(JCTypeCast tree) { /* SKIPPED */
	}

	public void visitTypeTest(JCInstanceOf tree) { /* SKIPPED */
	}

	public void visitIndexed(JCArrayAccess tree) { /* SKIPPED */
	}

	public void visitSelect(JCFieldAccess tree) { /* SKIPPED */
	}

	public void visitCapsuleLambda(JCCapsuleLambda tree,
			final com.sun.tools.javac.comp.Attr attr, Env<AttrContext> env,
			Resolve rs) {
		checkLambdaArguments(tree);
		transformCapsuleLambda(tree, attr, env, rs);
	}

	public void visitPrimitiveCapsuleLambda(JCPrimitiveCapsuleLambda tree,
			final com.sun.tools.javac.comp.Attr attr, Env<AttrContext> env,
			Resolve rs) {
		checkLambdaArguments(tree);
		transformCapsuleLambda(tree, attr, env, rs);
	}
	
	private void checkLambdaArguments(JCCapsuleLambda tree){
		boolean first = true;
		for(List<JCVariableDecl> l = tree.params; l.nonEmpty(); l=l.tail){
			JCVariableDecl exp = l.head;
			if(!first && syms.capsules.containsKey(names.fromString(exp.vartype.toString()))){
				log.error(exp.pos(), "capsule.type.free.variables.found");
			}
			first = false;
		}
	}
	
	private void checkLambdaArguments(JCPrimitiveCapsuleLambda tree){
		boolean first = true;
		for(List<JCVariableDecl> l = tree.params; l.nonEmpty(); l=l.tail){
			JCVariableDecl exp = l.head;
			if(!first && syms.capsules.containsKey(names.fromString(exp.vartype.toString()))){
				log.error(exp.pos(), "capsule.type.free.variables.found");
			}
			
		}
	}
	
	/**
	 * Make sure intracapsule calls are calling the $Original versions
	 * so that calls don't go through the queues again.
	 */
	private void transformCapsuleLambdaBody(JCTree tree, final Name capsuleName){
		class IntraCapsuleCallsTransformer extends TreeScanner {
			@Override
			public final void visitSelect(JCFieldAccess tree) {
				if (tree.selected.toString().equals(
						capsuleName.toString())) {
					tree.name = tree.name
							.append(names
									.fromString(PaniniConstants.PANINI_ORIGINAL_METHOD_SUFFIX));
				} else
					tree.selected.accept(this);
			}
		}
		IntraCapsuleCallsTransformer icct = new IntraCapsuleCallsTransformer();
		tree.accept(icct);
	}
	
	private void transformCapsuleLambda(JCTree tree,
			final com.sun.tools.javac.comp.Attr attr, Env<AttrContext> env,
			Resolve rs) {
		final JCTree lambda = tree;
		if (lambda instanceof JCCapsuleLambda) {
			transformCapsuleLambdaBody(tree,
					((JCCapsuleLambda) lambda).capsuleName);
			JCClassDecl lambdaDuck;
			lambdaDuck = createPaniniLambda((JCCapsuleLambda) tree, env);
			enter.classEnter(List.<JCClassDecl> of(lambdaDuck), env.outer);

			// Enumeration of lambda classes
			((JCCapsuleDecl) env.enclClass).lambdaExpressionCounts++;
			((JCCapsuleLambda) tree).newClass = true;
			((JCCapsuleLambda) tree).clazz = id(lambdaDuck.name);
			for (JCVariableDecl var : ((JCCapsuleLambda) tree).params) {
				((JCCapsuleLambda) tree).args = ((JCCapsuleLambda) tree).args
						.append(id(var.name));
			}
		} else if (tree instanceof JCPrimitiveCapsuleLambda) {
			transformCapsuleLambdaBody(tree,
					((JCPrimitiveCapsuleLambda) lambda).capsuleName);
			JCClassDecl lambdaDuck;
			lambdaDuck = createPrimitivePaniniLambda(
					(JCPrimitiveCapsuleLambda) tree, env);
			enter.classEnter(List.<JCClassDecl> of(lambdaDuck), env.outer);
//			System.out.println(lambdaDuck);// prints out resulting duck class
			ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
			for (JCVariableDecl var : ((JCPrimitiveCapsuleLambda) tree).params) {
				args.add(id(var.name));
			}

			String restypeString = ((JCPrimitiveCapsuleLambda) tree).restype
					.toString();

			// set up the lambda, switch to newClass.apply mode
			((JCPrimitiveCapsuleLambda) tree).meth = select(
					newt(PaniniConstants.DUCK_INTERFACE_NAME
							+ "$"
							+ restypeString
							+ "$"
							+ env.enclClass.name.toString()
							+ "$"
							+ ((JCCapsuleDecl) env.enclClass).lambdaExpressionCounts,
							args),
					getValue(((JCPrimitiveCapsuleLambda) tree).restype));
			((JCCapsuleDecl) env.enclClass).lambdaExpressionCounts++;
			((JCPrimitiveCapsuleLambda) tree).newClass = true;
		}
		tree.accept(attr);
	}
	
	
	private String getValue(JCExpression exp){
		if(exp instanceof JCIdent)
			return "toString";
		if(exp instanceof JCPrimitiveTypeTree)
		switch(((JCPrimitiveTypeTree) exp).getPrimitiveTypeKind()){
			case LONG:
				return "longValue";
			case BOOLEAN:
				return "booleanValue";
			case CHAR:
				return "charValue";
			case DOUBLE:
				return "doubleValue";
			case FLOAT:
				return "floatValue";
			case INT:
				return "intValue";
			case SHORT:
				return "shortValue";
			case BYTE:
				return "byteValue";
			default:
				return null;
		}
		return "";
	}

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

	public final void postVisitMethodDef(JCMethodDecl tree,
			Env<AttrContext> env, Resolve rs) {
		if (tree.body != null) {
			tree.accept(new ASTCFGBuilder(make));
		}

		MethodSymbol ms = tree.sym;
		ClassSymbol owner = (ClassSymbol) ms.owner;
		if ((owner.flags() & Flags.CAPSULE) != 0) {
			CapsuleProcedure cp = new CapsuleProcedure(owner, tree.name,
					ms.params);
			owner.capsule_info.procedures.put(ms, cp);
			if (ms.effect != null) {
				annotationProcessor.setEffects(tree, ms.effect);
				Attribute.Compound buf = annotate.enterAnnotation(
						tree.mods.annotations.last(), Type.noType, env);
				ms.attributes_field = ms.attributes_field.append(buf);
			}
		}
	}
	
	private final void addDelegationMethod(final JCCapsuleDecl tree, 
			final com.sun.tools.javac.comp.Attr attr, Env<AttrContext> env,
			Resolve rs){
		JCExpression extending = ((JCCapsuleDecl) tree.sym.capsule_info.parentCapsule.tree).implementing.head;
		int methodIndex = tree.publicMethods.size();
		if(((JCCapsuleDecl)tree.sym.capsule_info.parentCapsule.tree).delegationMethods.isEmpty())
			return;
		
		JCVariableDecl superCapsule;
		superCapsule = var(
				mods(Flags.PRIVATE | FINAL),
				PaniniConstants.PANINI_SUPERCAPSULE,
				extending,
				newt(extending+"$thread"));
		tree.defs = tree.defs.prepend(superCapsule);
		memberEnter.memberEnter(superCapsule, env);
		TreeCopier<Void> tc = new TreeCopier<Void>(make);
		for(JCMethodDecl m : ((JCCapsuleDecl)tree.sym.capsule_info.parentCapsule.tree).delegationMethods){
			MethodSymbol ms = m.sym;
			JCBlock body = null;
			ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
			for(VarSymbol v : ms.params){
				args.add(id(v.name));
			}
			ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
			for(JCVariableDecl v : m.params){
				params.add(tc.copy(v));
			}
			if (m.name.toString().contains(
					PaniniConstants.PANINI_ORIGINAL_METHOD_SUFFIX)) {
				if (ms.getReturnType().toString().equals("void"))
					body = body(es(apply(PaniniConstants.PANINI_SUPERCAPSULE,
							m.name.toString(), args)));
				else
					body = body(returnt(apply(PaniniConstants.PANINI_SUPERCAPSULE,
							m.name.toString(), args)));
			} else {
				String constantName = PaniniConstants.PANINI_METHOD_CONST
						+ m.name;
				if (m.params.nonEmpty())
					for (JCVariableDecl param : m.params) {
						constantName = constantName + "$" + param.vartype;
					}
				
				JCVariableDecl methodConstant;
				superCapsule = var(
						mods(Flags.PRIVATE | Flags.STATIC| FINAL),
						constantName,
						intt(),
						intlit(methodIndex++));
				tree.defs = tree.defs.prepend(superCapsule);
				memberEnter.memberEnter(superCapsule, env);
				
				ListBuffer<JCStatement> copyBody = new ListBuffer<JCStatement>();
				copyBody.append(make.Exec(make.Apply(List.<JCExpression> nil(),
						make.Ident(names.fromString(PaniniConstants.PANINI_PUSH)),
						List.<JCExpression> of(make
								.Ident(names.panini.PaniniDuckFuture)))));
				JCExpression duckType = enter.getDuckType(tree, m);
				args.prepend(make.Ident(names.fromString(constantName)));
				copyBody.prepend(make.Try(
						make.Block(
								0,
								List.<JCStatement> of(make.Exec(make.Assign(
										id(names.panini.PaniniDuckFuture),
										make.NewClass(
												null,
												List.<JCExpression> nil(),
												duckType,
												args.toList(),
												null))))),
						List.<JCCatch> of(make.Catch(
								make.VarDef(make.Modifiers(0),
										names.fromString("e"),
										make.Ident(names.fromString("Exception")),
										null),
								make.Block(
										0,
										List.<JCStatement> of(make.Throw(make.NewClass(
												null,
												List.<JCExpression> nil(),
												make.Ident(names
														.fromString("DuckException")),
												List.<JCExpression> of(make
														.Ident(names
																.fromString("e"))),
												null)))))), null));
				copyBody.prepend(make.VarDef(make.Modifiers(0),
						names.panini.PaniniDuckFuture, duckType,
						make.Literal(TypeTags.BOT, null)));
				if (!m.restype.toString().equals("void"))
					copyBody.append(enter.procedureReturnStatement(m));
				body = body(copyBody);
			}
			if(!tree.name.toString().contains("$thread"))
				body = body();
			JCMethodDecl method = method(mods(ms.flags_field &= ~Flags.ABSTRACT), 
					m.name, tc.copy(m.restype), params,body);
			if(!method.name.toString().contains(PaniniConstants.PANINI_ORIGINAL_METHOD_SUFFIX))
				tree.publicMethods = tree.publicMethods.append(method);
			tree.defs = tree.defs.append(method);
			memberEnter.memberEnter(method, env);
			tree.sym.members().enter(method.sym);
		}
	}
	
	private final void inheritanceDelegation(final JCCapsuleDecl tree,
			final com.sun.tools.javac.comp.Attr attr, Env<AttrContext> env,
			Resolve rs){
		JCExpression extending = tree.implementing.head;
		Symbol s = rs.findType(env, names.fromString(extending.toString()));
		if (tree.sym.isInterface()) {
			Entry element = s.members().elems;
			boolean lastOneIsOrg = false;
			while (element != null) {
				if (element.sym.toString().contains("$Original")) {
					JCMethodDecl method = make.MethodDef(
							(MethodSymbol) element.sym, null);
					tree.defs = tree.defs.append(method);
					tree.delegationMethods = tree.delegationMethods.append(method);
					lastOneIsOrg = true;
					memberEnter.memberEnter(method, env);
					tree.sym.members().enter(method.sym);
				} else if (lastOneIsOrg) {
					JCMethodDecl method = make.MethodDef(
							(MethodSymbol) element.sym, null);
					tree.defs = tree.defs.append(method);
					tree.delegationMethods = tree.delegationMethods.append(method);
					lastOneIsOrg = false;
					memberEnter.memberEnter(method, env);
					tree.sym.members().enter(method.sym);
				}
				element = element.sibling;
			}
		} else {
			addDelegationMethod(tree, attr, env, rs);
		}
//		tree.implementing = tree.implementing.append(tree.extending);
	}

	public final void visitCapsuleDef(final JCCapsuleDecl tree,
			final com.sun.tools.javac.comp.Attr attr, Env<AttrContext> env,
			Resolve rs) {
		tree.sym.capsule_info.connectedCapsules = tree.sym.capsule_info.connectedCapsules
				.appendList(tree.params);
		if((tree.implementing.size()>1&&tree.needsDelegation)||(!tree.sym.isInterface()&&tree.parentCapsule.delegationMethods.nonEmpty())){
			inheritanceDelegation(tree, attr, env, rs);
		}
		if (tree.needsDefaultRun) {
			List<JCClassDecl> wrapperClasses = generateClassWrappers(tree, env);
			enter.classEnter(wrapperClasses, env.outer);
//			 System.out.println(wrapperClasses);
			for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
				JCTree def = l.head;
				if (def.getTag() == Tag.METHODDEF) {
					JCMethodDecl mdecl = (JCMethodDecl) def;
					Type restype = ((MethodType) mdecl.sym.type).restype;
					ClassSymbol c = checkAndResolveReturnType(env, rs, restype);

					if (!mdecl.name.toString().contains("$Original")
							&& restype.isFinal()
							&& !c.toString().equals("java.lang.String")) {
						ListBuffer<JCStatement> statements = new ListBuffer<JCStatement>();
						for (List<JCStatement> stats = mdecl.body.stats; stats
								.nonEmpty(); stats = stats.tail) {
							JCStatement stat = stats.head;
							if (stat.getTag() != Tag.RETURN) {
								statements.append(stat);
							}
						}
						statements.append(make.Return(make.TypeCast(
								mdecl.restype, make.Apply(List
										.<JCExpression> nil(), make.Select(make
										.Ident(names.panini.PaniniDuckFuture),
										names.fromString("finalValue")), List
										.<JCExpression> nil()))));
						mdecl.body.stats = statements.toList();
					}
				}
			}
			attr.attribClassBody(env, tree.sym);
			if ((tree.sym.flags_field & TASK) != 0)
				tree.computeMethod.body = generateTaskCapsuleComputeMethodBody(tree);
			else
				tree.computeMethod.body = generateThreadCapsuleComputeMethodBody(tree);
			attr.attribStat(tree.computeMethod, env);
		} else {
			attr.attribClassBody(env, tree.sym);
			if (tree.computeMethod != null) {
				tree.computeMethod.body.stats = tree.computeMethod.body.stats
						.prepend(make.Exec(make.Apply(
								List.<JCExpression> nil(),
								make.Ident(names.panini.PaniniCapsuleInit),
								List.<JCExpression> nil())));
				if ((tree.sym.flags_field & ACTIVE) != 0) {
					// Wire the system
					tree.computeMethod.body.stats = tree.computeMethod.body.stats
							.prepend(make
									.Exec(createSimpleMethodCall(names.panini.InternalCapsuleWiring)));
					// Reference count disconnect()
					ListBuffer<JCStatement> blockStats = new ListBuffer<JCStatement>();
					blockStats = createCapsuleMemberDisconnects(tree.sym.capsule_info.connectedCapsules);
					ListBuffer<JCStatement> body = new ListBuffer<JCStatement>();
					body.add(make.Try(
							make.Block(0, tree.computeMethod.body.stats),
							List.<JCCatch> nil(), body(blockStats)));
					tree.computeMethod.body.stats = body.toList();
				}
				attr.attribStat(tree.computeMethod, env);
			}
			if ((tree.sym.flags_field & SERIAL) != 0
					|| (tree.sym.flags_field & MONITOR) != 0) {
				// For serial capsule version
				ListBuffer<JCStatement> blockStats = new ListBuffer<JCStatement>();
				blockStats = createCapsuleMemberDisconnects(tree.sym.capsule_info.connectedCapsules);
				ListBuffer<JCStatement> methodStats = new ListBuffer<JCStatement>();
				methodStats
						.append(make.Exec(make.Unary(
								JCTree.Tag.POSTDEC,
								make.Ident(names
										.fromString(PaniniConstants.PANINI_REF_COUNT)))));

				if (blockStats.size() > 0)
					methodStats
							.append(make.If(
									make.Binary(
											JCTree.Tag.EQ,
											make.Ident(names
													.fromString(PaniniConstants.PANINI_REF_COUNT)),
											make.Literal(TypeTags.INT,
													Integer.valueOf(0))), make
											.Block(0, blockStats.toList()),
									null));

				JCBlock body = make.Block(0, methodStats.toList());

				JCMethodDecl disconnectMeth = null;
				MethodSymbol msym = null;
				msym = new MethodSymbol(PUBLIC | FINAL | Flags.SYNCHRONIZED,
						names.panini.PaniniDisconnect, new MethodType(
								List.<Type> nil(), syms.voidType,
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

		for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
			JCTree def = l.head;
			if (def.getTag() == Tag.METHODDEF) {
				JCMethodDecl mdecl = (JCMethodDecl) def;
				for (List<JCVariableDecl> p = mdecl.params; p.nonEmpty(); p = p.tail) {
					JCVariableDecl param = p.head;
					if ((param.type.tsym.flags_field & Flags.CAPSULE) != 0
							&& !mdecl.name.toString().contains("$Original")) {
						log.error("procedure.argument.illegal", param,
								mdecl.name.toString(), tree.sym);
					}
				}
			} else if (def.getTag() == Tag.VARDEF) {
				JCVariableDecl vdecl = (JCVariableDecl) def;
				if ((vdecl.type.tsym.flags_field & Flags.CAPSULE) != 0)
					vdecl.mods.flags |= FINAL;
			}
		}
		pchk.checkStateInit(tree.sym, env);
	}
	
	private ListBuffer<JCStatement> createCapsuleMemberDisconnects(
			List<JCVariableDecl> params) {
		ListBuffer<JCStatement> blockStats = new ListBuffer<JCStatement>();
		for (JCVariableDecl jcVariableDecl : params) {
			if (jcVariableDecl.vartype.type.tsym.isCapsule()) {
				JCStatement stmt = make.Exec(make.Apply(List
						.<JCExpression> nil(), make.Select(make.TypeCast(make
						.Ident(names.fromString(PaniniConstants.PANINI_QUEUE)),
						make.Ident(jcVariableDecl.name)),
						names.panini.PaniniDisconnect), List
						.<JCExpression> nil()));

				blockStats.append(stmt);
			} else if (jcVariableDecl.vartype.type.tsym.name.toString()
					.equalsIgnoreCase("Array")) {
				if (((ArrayType) jcVariableDecl.vartype.type).elemtype.tsym
						.isCapsule()) {
					ListBuffer<JCStatement> loopBody = new ListBuffer<JCStatement>();
					JCVariableDecl arraycache = make.VarDef(make.Modifiers(0),
							names.fromString("index$"), make.TypeIdent(INT),
							make.Literal(0));
					JCBinary cond = make.Binary(
							LT,
							make.Ident(names.fromString("index$")),
							make.Select(make.Ident(jcVariableDecl.name),
									names.fromString("length")));
					JCUnary unary = make.Unary(PREINC,
							make.Ident(names.fromString("index$")));
					JCExpressionStatement step = make.Exec(unary);
					loopBody.add(make.Exec(make.Apply(
							List.<JCExpression> nil(),
							make.Select(
									make.TypeCast(
											make.Ident(names
													.fromString(PaniniConstants.PANINI_QUEUE)),
											make.Indexed(
													make.Ident(jcVariableDecl.name),
													make.Ident(names
															.fromString("index$")))),
									names.panini.PaniniDisconnect), List
									.<JCExpression> nil())));
					JCForLoop floop = make.ForLoop(
							List.<JCStatement> of(arraycache), cond,
							List.of(step), make.Block(0, loopBody.toList()));
					blockStats.append(floop);
				}
			}
		}
		return blockStats;
	}

	private void initRefCount(Map<Name, Name> variables,
			Map<Name, JCFieldAccess> refCountStats,
			ListBuffer<JCStatement> assigns, SystemGraph sysGraph,
			Env<AttrContext> env, Map<Name, Integer> aliasRefCounts) {
		Set<Name> vars = sysGraph.nodes.keySet();
		final Name _this = names._this;
		final Name paniniRCField = names.panini.PaniniRefCountField;
		for (Name vdeclName : vars) {
			// Reference count update
			int refCount = 0;
			refCount = sysGraph.nodes.get(vdeclName).indegree;
			if (aliasRefCounts.containsKey(vdeclName)) {
				refCount += aliasRefCounts.get(vdeclName);
			}
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
			JCAssignOp refCountAssignOp = make.Assignop(Tag.PLUS_ASG,
					accessStat, intlit(refCount));
			JCExpressionStatement refCountAssignStmt = make
					.Exec(refCountAssignOp);
			assigns.append(refCountAssignStmt);
		}
	}

	public final void visitSystemDef(JCDesignBlock tree, Resolve rs,
			com.sun.tools.javac.comp.Attr jAttr, // Javac Attributer.
			Env<AttrContext> env, boolean doGraphs) {
		tree.sym.flags_field = pchk
				.checkFlags(tree, tree.sym.flags(), tree.sym);
		// Use the scope of the out capsule, not the current system decl.
		Scope scope = enterSystemScope(env);
		moveWiringDecls(tree, scope);
		ListBuffer<JCStatement> decls;
		ListBuffer<JCStatement> inits;
		ListBuffer<JCStatement> assigns;
		ListBuffer<JCStatement> starts;
		SystemGraph sysGraph;

		DesignDeclRewriter interp = new DesignDeclRewriter(make, log,
				env.enclClass.sym);
		JCDesignBlock rewritenTree = interp.rewrite(tree);
		// we do not want to go on to generating the main method if there are
		// errors in the design decl
		if (log.nerrors > 0)
			return;
		DesignDeclTransformer mt = new DesignDeclTransformer(syms, names,
				types, log, rs, env, make, systemGraphBuilder);
		mt.setCapsuleAliases(capsuleAliases); // gc-fix
		rewritenTree = mt.translate(rewritenTree);

		// Check for cyclic references and report it
		pchk.checkCycleRepeat(mt.sysGraph, names._this, env);

		// pull data structures back out for reference here.
		decls = mt.decls;
		inits = mt.inits;
		assigns = mt.assigns;
		starts = mt.starts;
		sysGraph = mt.sysGraph;

		if (rewritenTree.hasTaskCapsule)
			processSystemAnnotation(rewritenTree, inits, env);

		Map<Name, Integer> aliasRefCounts = mt.aliasRefCounts;
		initRefCount(mt.variables, mt.refCountStats, assigns, sysGraph, env,
				aliasRefCounts);

		// attribute the new statement.
		ListBuffer<JCStatement> toAttr = new ListBuffer<JCTree.JCStatement>();
		toAttr.addAll(decls);
		toAttr.addAll(inits);
		toAttr.addAll(assigns);
		toAttr.addAll(starts);
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
		mainStmts = decls.appendList(inits).appendList(assigns)
				.appendList(starts).toList();

		tree.sysGraph = sysGraph;
		this.designBlocks = this.designBlocks.append(tree);

		// replace the systemDef/wiring block with the new body.
		tree.body.stats = mainStmts;
//		 System.out.println(tree.sysGraph);
	}

	public final void postVisitSystemDefs(Env<AttrContext> env) {
		for (List<JCDesignBlock> l = this.designBlocks; l.nonEmpty(); l = l.tail) {
			JCDesignBlock tree = l.head;
			postVisitSystemDef(tree, env);
		}
		this.designBlocks = List.<JCDesignBlock> nil();
	}

	private final void postVisitSystemDef(JCDesignBlock tree,
			Env<AttrContext> env) {
		systemGraphBuilder.completeEdges(tree.sysGraph, annotationProcessor,
				env, rs);

		// Sequential consistency detection
		SeqConstCheckAlgorithm sca = ConsistencyUtil.createChecker(seqConstAlg,
				tree.sysGraph, log);
		sca.potentialPathCheck();
	}

	public final void visitProcDef(JCProcDecl tree) {
		Type restype = ((MethodType) tree.sym.type).restype;
		if ((restype.tsym.flags_field & Flags.CAPSULE) == 1) {
			log.error(tree.pos(), "procedure.restype.illegal.capsule");
		}
		tree.switchToMethod();
	}

	public final void visitStateDef(JCStateDecl tree) {
		if (tree.type.tsym.isCapsule()) {
			log.error(tree.pos(), "states.with.capsule.type.error");
		}

		// It's attributed. Make it look like a regular variable
		tree.switchToVar();
	}

	public boolean visitLambda(JCLambda tree) {

		return true;
	}

	// Helper functions
	private void processSystemAnnotation(JCDesignBlock tree,
			ListBuffer<JCStatement> stats, Env<AttrContext> env) {
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
		stats.prepend(make.Try(make.Block(0,
				List.<JCStatement> of(make.Exec(make.Apply(List
						.<JCExpression> nil(), make.Select(make.Ident(names
						.fromString(PaniniConstants.PANINI_CAPSULE_TASK)),
						names.fromString(PaniniConstants.PANINI_INIT)), List
						.<JCExpression> of(make.Literal(numberOfPools)))))),
				List.<JCCatch> of(make.Catch(make.VarDef(make.Modifiers(0),
						names.fromString("e"),
						make.Ident(names.fromString("Exception")), null), make
						.Block(0, List.<JCStatement> nil()))), null));
	}

	/**
	 * @param tree
	 * @return
	 */
	private List<JCVariableDecl> extractWiringBlockDecls(JCDesignBlock tree) {
		class VarDeclCollector extends TreeScanner { // Helper visitor to
														// collect var defs.
			final ListBuffer<JCVariableDecl> varDecls = new ListBuffer<JCVariableDecl>();

			@Override
			public final void visitVarDef(JCVariableDecl tree) {
				Type t = tree.vartype.type;
				if (t.tsym.isCapsule()
						|| (types.isArray(t) && types.elemtype(t).tsym
								.isCapsule())) {
					varDecls.add(tree);
				}
			}
		}
		VarDeclCollector vdc = new VarDeclCollector();
		tree.accept(vdc);
		return vdc.varDecls.toList();
	}

	/**
	 * Move the capsule declarations in a wiring/design block out to the 'field'
	 * scope and record the capsules decls in {@link ClassSymbol#capsule_info
	 * #connectedCapsules}
	 * 
	 * @param wire
	 * @param capScope
	 */
	private void moveWiringDecls(JCDesignBlock wire, Scope capScope) {
		List<JCVariableDecl> capsuleDecls = extractWiringBlockDecls(wire);
		ClassSymbol capSym = wire.sym.ownerCapsule();
		capSym.capsule_info.connectedCapsules = capSym.capsule_info.connectedCapsules
				.appendList(capsuleDecls);

		// Enter the symbols into the capsule scope.
		// Allows the symbols to be visible for other procedures
		// and allows the symbols to be emitted as fields in the bytecode
		for (List<JCVariableDecl> l = capsuleDecls; l.nonEmpty(); l = l.tail) {
			JCVariableDecl v = l.head;
			v.sym.owner = capScope.owner;
			jchk.checkUnique(v.pos(), v.sym, capScope);
			capScope.enter(v.sym);
		}

		// TODO: Generics are being annoying. Find a way to not copy the list.
		ListBuffer<JCTree> capFields = new ListBuffer<JCTree>();
		for (List<JCVariableDecl> l = capsuleDecls; l.nonEmpty(); l = l.tail) {
			capFields.add(l.head);
			// Mark as private. Do not mark synthetic. Will cause other
			// name resolution to fail.
			l.head.sym.flags_field |= Flags.PRIVATE;
			// Update the AST Modifiers for pretty printing.
			l.head.mods = make.Modifiers(l.head.sym.flags_field);
		}

		// Copy the capsules over to the tree defs so they show up with
		// print-flat flag.
		JCCapsuleDecl tree = (JCCapsuleDecl) wire.sym.owner.tree;
		tree.defs = tree.defs.prependList(capFields.toList());
	}

	/**
	 * Get back to the 'class' level members scope from the current environment.
	 * Fails if a class symbol cannot be found.
	 * 
	 * @param env
	 */
	protected Scope enterSystemScope(Env<AttrContext> env) {
		while (env != null && !env.tree.hasTag(JCTree.Tag.CAPSULEDEF)) {
			env = env.next;
		}

		if (env != null) {
			return ((JCClassDecl) env.tree).sym.members_field;
		}
		Assert.error();
		return null;
	}
}
