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
 * Contributor(s): Rex Fernando
 */

package org.paninij.analysis;

import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeScanner;
import static com.sun.tools.javac.code.Flags.*;

import java.util.ArrayList;
import java.util.LinkedList;

public class CFGNodeBuilder extends TreeScanner {
	private JCCapsuleDecl capsule;
	private JCMethodDecl m;
	private CFG cfg;
	private ArrayList<CFGNodeImpl> currentStartNodes, currentEndNodes,
		currentExcEndNodes;
	private final ArrayList<CFGNodeImpl> emptyList = new ArrayList<CFGNodeImpl>(0);
	private boolean lhs = false;
	public Names names;

	public static class TodoItem {
		public JCMethodInvocation tree;
		public JCMethodDecl method;
		public TodoItem(JCMethodInvocation tree, JCMethodDecl method) {
			this.tree = tree;
			this.method = method;
		}
	}

	public static LinkedList<TodoItem> callGraphTodos =
		new LinkedList<TodoItem>();

	public void buildNodes(JCCapsuleDecl capsule, JCMethodDecl m, CFG cfg) {
		this.capsule = capsule;
		this.m = m;
		this.cfg = cfg;

		scan(m.body);

		cfg.startNode = cfg.nodeForTree(m.body);
	}

	public void visitTopLevel(JCCompilationUnit that)    { Assert.error(); }
	public void visitImport(JCImport that)               { Assert.error(); }
	public void visitProcDef(JCProcDecl that)            { Assert.error(); }
	public void visitMethodDef(JCMethodDecl that)        { Assert.error(); }
	public void visitLetExpr(LetExpr that)               { Assert.error(); }
	public void visitAssert(JCAssert that)               { Assert.error(); }
	public void visitAnnotation(JCAnnotation that)       { Assert.error(); }
	public void visitModifiers(JCModifiers that)         { Assert.error(); }
	public void visitErroneous(JCErroneous that)         { Assert.error(); }

	public void visitTypeIdent(JCPrimitiveTypeTree that) { Assert.error(); }
	public void visitTypeArray(JCArrayTypeTree that)     { Assert.error(); }
	public void visitTypeApply(JCTypeApply that)         { Assert.error(); }
	public void visitTypeUnion(JCTypeUnion that)         { Assert.error(); }
	public void visitTypeParameter(JCTypeParameter that) { Assert.error(); }
	public void visitWildcard(JCWildcard that)           { Assert.error(); }
	public void visitTypeBoundKind(TypeBoundKind that)   { Assert.error(); }

	public void visitClassDef(JCClassDecl that) { singleton(that); }
	public void visitSkip(JCSkip that) { singleton(that); }
	public void visitLabelled(JCLabeledStatement that) { singleton(that); }
	public void visitBreak(JCBreak that) { singleton(that); }
	public void visitContinue(JCContinue that) { singleton(that); }
	public void visitLiteral(JCLiteral that) { singleton(that); }
	public void visitIdent(JCIdent that) { singleton(that); }

	public void visitProcApply(JCProcInvocation tree) {
		visitApply(tree);
	}

	public void visitFree(JCFree tree) {
		scan(tree.exp);
	}

	private void addNode(CFGNodeImpl node) {
		node.startNodes = currentStartNodes;
		node.endNodes = currentEndNodes;
		node.excEndNodes = currentExcEndNodes;
		node.lhs = lhs;

		cfg.add(node);
	}

	public void visitVarDef(JCVariableDecl tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);
		JCExpression init = tree.init;
		if (init != null) {
			tree.init.accept(this);
		} else {
			currentStartNodes = new ArrayList<CFGNodeImpl>(1);
			currentStartNodes.add(node);
		}

		currentEndNodes = new ArrayList<CFGNodeImpl>(1);
		currentEndNodes.add(node);

		currentExcEndNodes = emptyList;

		addNode(node);
	}


	public void visitBlock(JCBlock tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);
		List<JCStatement> stats = tree.stats;
		visitStatements(stats);

		addNode(node);
	}

	public void visitDoLoop(JCDoWhileLoop tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		ArrayList<CFGNodeImpl> finalEndNodes = new ArrayList<CFGNodeImpl>();
		ArrayList<CFGNodeImpl> finalExcEndNodes = new ArrayList<CFGNodeImpl>();
		tree.body.accept(this);

		ArrayList<CFGNodeImpl> bodyStartNodes =
			new ArrayList<CFGNodeImpl>(this.currentStartNodes);
		ArrayList<CFGNodeImpl> bodyEndNodes =
			new ArrayList<CFGNodeImpl>(this.currentEndNodes);
		ArrayList<CFGNodeImpl> bodyExcEndNodes =
			new ArrayList<CFGNodeImpl>(this.currentExcEndNodes);
		ArrayList<CFGNodeImpl> breaks =
			getBreaks(bodyExcEndNodes);
		bodyEndNodes.addAll(breaks);
		bodyExcEndNodes.removeAll(breaks);

		tree.cond.accept(this);
		finalEndNodes.addAll(bodyEndNodes);
		finalEndNodes.addAll(this.currentEndNodes);
		finalExcEndNodes.addAll(bodyExcEndNodes);
		finalExcEndNodes.addAll(this.currentExcEndNodes);

		this.currentStartNodes = bodyStartNodes;
		this.currentEndNodes = finalEndNodes;
		this.currentExcEndNodes = finalExcEndNodes;


		addNode(node);
	}

	public void visitWhileLoop(JCWhileLoop tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		tree.cond.accept(this);
		ArrayList<CFGNodeImpl> condStartNodes = this.currentStartNodes;
		ArrayList<CFGNodeImpl> condEndNodes = this.currentEndNodes;
		ArrayList<CFGNodeImpl> condExcEndNodes = this.currentExcEndNodes;

		tree.body.accept(this);
		ArrayList<CFGNodeImpl> bodyEndNodes =
			new ArrayList<CFGNodeImpl>(this.currentEndNodes);
		ArrayList<CFGNodeImpl> bodyExcEndNodes =
			new ArrayList<CFGNodeImpl>(this.currentExcEndNodes);

		ArrayList<CFGNodeImpl> breaks = getBreaks(bodyExcEndNodes);
		bodyEndNodes.addAll(breaks);
		bodyExcEndNodes.removeAll(breaks);

		ArrayList<CFGNodeImpl> finalEndNodes = new ArrayList<CFGNodeImpl>();
		ArrayList<CFGNodeImpl> finalExcEndNodes = new ArrayList<CFGNodeImpl>();
		finalEndNodes.addAll(bodyEndNodes);
		finalEndNodes.addAll(condEndNodes);
		finalExcEndNodes.addAll(bodyExcEndNodes);
		finalExcEndNodes.addAll(condExcEndNodes);


		this.currentStartNodes = condStartNodes;
		this.currentEndNodes = finalEndNodes;
		this.currentExcEndNodes = finalExcEndNodes;


		addNode(node);
	}

	public void visitForLoop(JCForLoop tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);
		if (tree.init.isEmpty()) {
			ArrayList<CFGNodeImpl> finalEndNodes = new ArrayList<CFGNodeImpl>();

			if (tree.cond != null) {
				tree.cond.accept(this);
				finalEndNodes.addAll(currentEndNodes);

				ArrayList<CFGNodeImpl> currentStartNodes = this.currentStartNodes;
				tree.body.accept(this);

				ArrayList<CFGNodeImpl> currentEndNodes =
					new ArrayList<CFGNodeImpl>(this.currentEndNodes);
				ArrayList<CFGNodeImpl> currentExcEndNodes =
					new ArrayList<CFGNodeImpl>(this.currentExcEndNodes);

				ArrayList<CFGNodeImpl> breaks = getBreaks(currentExcEndNodes);
				currentEndNodes.addAll(breaks);
				currentExcEndNodes.removeAll(breaks);

				finalEndNodes.addAll(currentEndNodes);

				if (!tree.step.isEmpty()) {
					visitStatements(tree.step);
					finalEndNodes.addAll(this.currentEndNodes);
				}

				this.currentStartNodes = currentStartNodes;
				this.currentEndNodes = finalEndNodes;
				this.currentExcEndNodes = currentExcEndNodes;

				addNode(node);
			} else {/*tree.cond == null*/
				tree.body.accept(this);

			ArrayList<CFGNodeImpl> currentStartNodes = this.currentStartNodes;
			ArrayList<CFGNodeImpl> currentEndNodes =
				new ArrayList<CFGNodeImpl>(this.currentEndNodes);
			ArrayList<CFGNodeImpl> currentExcEndNodes =
				new ArrayList<CFGNodeImpl>(this.currentExcEndNodes);

			ArrayList<CFGNodeImpl> breaks = getBreaks(currentExcEndNodes);
			currentEndNodes.addAll(breaks);
			currentExcEndNodes.removeAll(breaks);
			finalEndNodes.addAll(currentEndNodes);

			if (!tree.step.isEmpty()) {
				visitStatements(tree.step);
				finalEndNodes.addAll(this.currentEndNodes);
			}

			this.currentStartNodes = currentStartNodes;
			this.currentEndNodes = finalEndNodes;
			this.currentExcEndNodes = currentExcEndNodes;

			addNode(node);
			}
		} else {/*!init.isEmpty()*/
			ArrayList<CFGNodeImpl> finalEndNodes = new ArrayList<CFGNodeImpl>();


			visitStatements(tree.init);
			ArrayList<CFGNodeImpl> currentStartNodes = this.currentStartNodes;

			if (tree.cond != null) {
				tree.cond.accept(this);
			}

			tree.body.accept(this);
			ArrayList<CFGNodeImpl> currentEndNodes =
				new ArrayList<CFGNodeImpl>(this.currentEndNodes);
			ArrayList<CFGNodeImpl> currentExcEndNodes =
				new ArrayList<CFGNodeImpl>(this.currentExcEndNodes);
			ArrayList<CFGNodeImpl> breaks = getBreaks(currentExcEndNodes);
			currentEndNodes.addAll(breaks);
			currentExcEndNodes.removeAll(breaks);
			finalEndNodes.addAll(currentEndNodes);

			if (!tree.step.isEmpty()) {
				visitStatements(tree.step);
				finalEndNodes.addAll(this.currentEndNodes);
			}

			this.currentStartNodes = currentStartNodes;
			this.currentEndNodes = finalEndNodes;
			this.currentExcEndNodes = currentExcEndNodes;

			addNode(node);
		}
	}

	public void visitForeachLoop(JCEnhancedForLoop tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		tree.expr.accept(this);
		ArrayList<CFGNodeImpl> currentStartNodes = this.currentStartNodes;

		tree.body.accept(this);
		this.currentStartNodes = currentStartNodes;
		this.currentEndNodes = new ArrayList<CFGNodeImpl>(currentEndNodes);
		this.currentEndNodes.add(node);

		addNode(node);
	}

	public void visitSwitch(JCSwitch tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		tree.selector.accept(this);
		ArrayList<CFGNodeImpl> currentStartNodes = this.currentStartNodes;
		ArrayList<CFGNodeImpl> finalEndNodes = new ArrayList<CFGNodeImpl>();
		finalEndNodes.addAll(currentEndNodes);

		visitStatements(tree.cases);

		this.currentStartNodes = currentStartNodes;
		ArrayList<CFGNodeImpl> currentEndNodes =
			new ArrayList<CFGNodeImpl>(this.currentEndNodes);
		ArrayList<CFGNodeImpl> currentExcEndNodes =
			new ArrayList<CFGNodeImpl>(this.currentExcEndNodes);

		ArrayList<CFGNodeImpl> breaks = getBreaks(currentExcEndNodes);
		currentEndNodes.addAll(breaks); currentExcEndNodes.removeAll(breaks);
		finalEndNodes.addAll(currentEndNodes);

		this.currentStartNodes = currentStartNodes;
		this.currentEndNodes = finalEndNodes;
		this.currentExcEndNodes = currentExcEndNodes;

		addNode(node);
	}

	public void visitCase(JCCase tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		tree.pat.accept(this);
		ArrayList<CFGNodeImpl> currentStartNodes = this.currentStartNodes;

		visitStatements(tree.stats);

		this.currentStartNodes = currentStartNodes;

		addNode(node);
	}

	public void visitSynchronized(JCSynchronized tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		tree.lock.accept(this);
		ArrayList<CFGNodeImpl> currentStartNodes = this.currentStartNodes;

		tree.body.accept(this);
		this.currentStartNodes = currentStartNodes;

		addNode(node);
	}

	public void visitTry(JCTry tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		tree.body.accept(this);
		ArrayList<CFGNodeImpl> currentStartNodes = this.currentStartNodes;

		ArrayList<CFGNodeImpl> finalEndNodes = new ArrayList<CFGNodeImpl>();
		ArrayList<CFGNodeImpl> finalExcEndNodes = new ArrayList<CFGNodeImpl>();
		finalEndNodes.addAll(currentEndNodes);
		finalExcEndNodes.addAll(currentExcEndNodes);

		List<JCCatch> catchers = tree.catchers;
		JCBlock finalizer = tree.finalizer;

		if (catchers.isEmpty()) {
			finalizer.accept(this);

			this.currentStartNodes = currentStartNodes;
		} else {
			visitParallelStatements(catchers);

			if (finalizer != null) {
				finalizer.accept(this);
				this.currentStartNodes = currentStartNodes;
				/* TODO fix the logic for the try block, where the finalizer 
				 * could go outside a loop. */
				 node.startNodes = currentStartNodes;
				 node.endNodes = currentEndNodes;
				 node.excEndNodes = currentExcEndNodes;
			} else {
				this.currentStartNodes = currentStartNodes;
				finalEndNodes.addAll(currentEndNodes);
				currentEndNodes = finalEndNodes;
				finalExcEndNodes.addAll(currentExcEndNodes);
				currentExcEndNodes = finalExcEndNodes;
			}
		}
		addNode(node);
	}

	public void visitCatch(JCCatch tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		tree.param.accept(this);
		ArrayList<CFGNodeImpl> finalStartNodes = currentStartNodes;
		ArrayList<CFGNodeImpl> finalExcEndNodes = new ArrayList<CFGNodeImpl>();
		finalExcEndNodes.addAll(currentExcEndNodes);

		tree.body.accept(this);

		currentStartNodes = finalStartNodes;

		// what about finalExcEndNodes?
		addNode(node);
	}

	public void visitConditional(JCConditional tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		tree.cond.accept(this);
		ArrayList<CFGNodeImpl> currentStartNodes = this.currentStartNodes;

		ArrayList<CFGNodeImpl> finalEndNodes = new ArrayList<CFGNodeImpl>();
		ArrayList<CFGNodeImpl> finalExcEndNodes = new ArrayList<CFGNodeImpl>();
		tree.truepart.accept(this);
		finalEndNodes.addAll(this.currentEndNodes);
		finalExcEndNodes.addAll(this.currentExcEndNodes);

		tree.falsepart.accept(this);
		finalEndNodes.addAll(this.currentEndNodes);
		finalExcEndNodes.addAll(this.currentExcEndNodes);

		this.currentStartNodes = currentStartNodes;
		this.currentEndNodes = finalEndNodes;
		this.currentExcEndNodes = finalExcEndNodes;

		addNode(node);
	}

	public void visitIf(JCIf tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		tree.cond.accept(this);
		ArrayList<CFGNodeImpl> currentStartNodes = this.currentStartNodes;
		ArrayList<CFGNodeImpl> currentEndNodes = this.currentEndNodes;

		ArrayList<CFGNodeImpl> finalEndNodes = new ArrayList<CFGNodeImpl>();
		ArrayList<CFGNodeImpl> finalExcEndNodes = new ArrayList<CFGNodeImpl>();

		tree.thenpart.accept(this);
		finalEndNodes.addAll(this.currentEndNodes);
		finalExcEndNodes.addAll(currentExcEndNodes);

		if (tree.elsepart!=null) {
			tree.elsepart.accept(this);
			finalEndNodes.addAll(this.currentEndNodes);
			finalExcEndNodes.addAll(currentExcEndNodes);
		} else {
			finalEndNodes.addAll(currentEndNodes);
		}

		this.currentStartNodes = currentStartNodes;
		this.currentEndNodes = finalEndNodes;
		this.currentExcEndNodes = finalExcEndNodes;

		addNode(node);
	}

	public void visitExec(JCExpressionStatement tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		tree.expr.accept(this);

		node.startNodes = currentStartNodes;
		node.endNodes = currentEndNodes;
		node.excEndNodes = currentExcEndNodes;
		addNode(node);
	}

	public void visitReturn(JCReturn tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		if (tree.expr != null) {
			tree.expr.accept(this);
		}

		currentEndNodes = emptyList;
		currentExcEndNodes = new ArrayList<CFGNodeImpl>(1);
		currentExcEndNodes.add(node);

		addNode(node);
	}

	public void visitThrow(JCThrow tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		tree.expr.accept(this);
		currentEndNodes = emptyList;
		currentExcEndNodes = new ArrayList<CFGNodeImpl>(1);
		currentExcEndNodes.add(node);
		addNode(node);
	}

	public void visitApply(JCMethodInvocation tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		tree.meth.accept(this);

		ArrayList<CFGNodeImpl> startNodes = currentStartNodes;

		if (tree.args.isEmpty()) {
			//			currentStartNodes = new ArrayList<CFGNode>(1);
			//			currentStartNodes.add(node);
		} else {
			visitStatements(tree.args);
		}

		currentStartNodes = startNodes;

		currentEndNodes = new ArrayList<CFGNodeImpl>(1);
		currentEndNodes.add(node);
		currentExcEndNodes = emptyList;
		addNode(node);


		// building call graph
		if (capsule != null) { // is a capsule
			if (TreeInfo.symbol(tree.meth) != null) {
				MethodSymbol methSym = (MethodSymbol)TreeInfo.symbol(tree.meth);
				if ((methSym.flags() & PRIVATE) == 0 &&
						methSym.owner.isCapsule) {
					VarSymbol capsuleField = capsuleField(tree);
					if (methSym.owner == capsule.sym) {
						String methodName =
							TreeInfo.symbol(tree.meth).toString();
						methodName = methodName.substring(0,
								methodName.indexOf("(")) + "$Original";
						MethodSymbol origMethSym =
							(MethodSymbol)((ClassSymbol)methSym.owner).
							members_field.lookup(names.fromString(methodName)).
							sym;
						m.sym.calledMethods.add(new MethodSymbol.MethodInfo(
								origMethSym, capsuleField));
						origMethSym.callerMethods.add(m.sym);
					} else {
						callGraphTodos.add(new TodoItem(tree, m));
					}
				} else {
					m.sym.calledMethods.add(
							new MethodSymbol.MethodInfo(methSym));
					methSym.callerMethods.add(m.sym);
				}
			}
		} else { // is a library
			if (TreeInfo.symbol(tree.meth) != null) { 
				MethodSymbol methSym = (MethodSymbol)TreeInfo.symbol(tree.meth);
				m.sym.calledMethods.add(new MethodSymbol.MethodInfo(methSym));
				methSym.callerMethods.add(m.sym);
			}
		}
	}

	public static VarSymbol capsuleField(JCMethodInvocation tree) {
		if (tree.meth instanceof JCFieldAccess) {
			if (((JCFieldAccess)tree.meth).selected instanceof JCIdent) {
				VarSymbol s =
					(VarSymbol)TreeInfo.symbol(
							((JCFieldAccess)tree.meth).selected);
				if (!s.name.toString().equals("this")) {
					return (VarSymbol)TreeInfo.symbol(
							((JCFieldAccess)tree.meth).selected);
				}
			} else if (((JCFieldAccess)tree.meth).selected.getTag() ==
					Tag.INDEXED) {
				JCArrayAccess aa =
					(JCArrayAccess)((JCFieldAccess)tree.meth).selected;
				return (VarSymbol)TreeInfo.symbol(aa.indexed);
			}
		}
		return null;
	}

	public void visitNewClass(JCNewClass tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		if (tree.args.isEmpty()) {
			currentStartNodes = new ArrayList<CFGNodeImpl>(1);
			currentStartNodes.add(node);
		} else {
			visitStatements(tree.args);
		}

		currentEndNodes = new ArrayList<CFGNodeImpl>(1);
		currentEndNodes.add(node);
		currentExcEndNodes = emptyList;
		addNode(node);
	}

	public void visitNewArray(JCNewArray tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		List<JCExpression> dims = tree.dims;
		List<JCExpression> elems = tree.elems;
		ArrayList<CFGNodeImpl> finalExcEndNodes = new ArrayList<CFGNodeImpl>();
		ArrayList<CFGNodeImpl> finalStartNodes = null;
		if (!dims.isEmpty()) {
			visitStatements(dims);
			finalExcEndNodes.addAll(currentExcEndNodes);
			finalStartNodes = currentStartNodes;
		}

		if (elems != null) {
			if (!elems.isEmpty()) {
				visitStatements(elems);
				finalExcEndNodes.addAll(currentExcEndNodes);
			}
		}

		currentEndNodes = new ArrayList<CFGNodeImpl>(1);
		currentEndNodes.add(node);

		currentExcEndNodes = finalExcEndNodes;

		if (finalStartNodes == null) {
			currentStartNodes = new ArrayList<CFGNodeImpl>(1);
			currentStartNodes.add(node);
		} else { currentStartNodes = finalStartNodes; }

		node.startNodes = currentStartNodes;
		node.endNodes = currentEndNodes;
		node.excEndNodes = currentExcEndNodes;
		addNode(node);
	}

	public void visitParens(JCParens tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		tree.expr.accept(this);
		addNode(node);
	}

	public void visitAssign(JCAssign tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		lhs = true;
		tree.lhs.accept(this);
		lhs = false;
		ArrayList<CFGNodeImpl> currentStartNodes = this.currentStartNodes;
		tree.rhs.accept(this);

		currentEndNodes = new ArrayList<CFGNodeImpl>(1);
		currentEndNodes.add(node);

		this.currentStartNodes = currentStartNodes;
		addNode(node);
	}

	public void visitAssignop(JCAssignOp tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		lhs = true;
		tree.lhs.accept(this);
		lhs = false;
		ArrayList<CFGNodeImpl> currentStartNodes = this.currentStartNodes;
		tree.rhs.accept(this);

		currentEndNodes = new ArrayList<CFGNodeImpl>(1);
		currentEndNodes.add(node);

		this.currentStartNodes = currentStartNodes;
		addNode(node);
	}

	public void visitUnary(JCUnary tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		tree.arg.accept(this);

		currentEndNodes = new ArrayList<CFGNodeImpl>(1);
		currentEndNodes.add(node);

		node.startNodes = currentStartNodes;
		node.endNodes = currentEndNodes;
		node.excEndNodes = currentExcEndNodes;
		addNode(node);
	}

	public void visitBinary(JCBinary tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		tree.lhs.accept(this);
		ArrayList<CFGNodeImpl> currentStartNodes = this.currentStartNodes;
		tree.rhs.accept(this);

		currentEndNodes = new ArrayList<CFGNodeImpl>(1);
		currentEndNodes.add(node);

		this.currentStartNodes = currentStartNodes;
		addNode(node);
	}

	public void visitTypeCast(JCTypeCast tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		tree.expr.accept(this);

		currentEndNodes = new ArrayList<CFGNodeImpl>(1);
		currentEndNodes.add(node);

		addNode(node);
	}

	public void visitTypeTest(JCInstanceOf tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		tree.expr.accept(this);

		currentEndNodes = new ArrayList<CFGNodeImpl>(1);
		currentEndNodes.add(node);

		addNode(node);
	}

	public void visitIndexed(JCArrayAccess tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);
		tree.indexed.accept(this);

		ArrayList<CFGNodeImpl> currentStartNodes = this.currentStartNodes;
		ArrayList<CFGNodeImpl> finalExcEndNodes = new ArrayList<CFGNodeImpl>();
		finalExcEndNodes.addAll(currentExcEndNodes);

		tree.index.accept(this);

		finalExcEndNodes.addAll(currentExcEndNodes);

		currentEndNodes = new ArrayList<CFGNodeImpl>(1);
		currentEndNodes.add(node);

		currentExcEndNodes = finalExcEndNodes;
		this.currentStartNodes = currentStartNodes;

		addNode(node);
	}

	public void visitSelect(JCFieldAccess tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);
		tree.selected.accept(this);

		currentEndNodes = new ArrayList<CFGNodeImpl>(1);
		currentEndNodes.add(node);

		currentExcEndNodes = emptyList;

		node.startNodes = currentStartNodes;
		node.endNodes = currentEndNodes;
		node.excEndNodes = currentExcEndNodes;
		addNode(node);
	}

	public void visitTree(JCTree tree) {
		Assert.error();
	}

	public void singleton(JCTree tree) {
		CFGNodeImpl node = new CFGNodeImpl(tree);

		currentEndNodes = new ArrayList<CFGNodeImpl>(1);
		currentEndNodes.add(node);
		currentExcEndNodes = emptyList;

		currentStartNodes = new ArrayList<CFGNodeImpl>(1);
		currentStartNodes.add(node);

		addNode(node);
	}

	private void visitStatements(List<? extends JCTree> statements) {
		ArrayList<CFGNodeImpl> finalExcEndNodes = new ArrayList<CFGNodeImpl>();
		JCTree head = statements.head;
		if (head != null) {
			List<? extends JCTree> tail = statements.tail;
			head.accept(this);
			ArrayList<CFGNodeImpl> currentStartNodes = this.currentStartNodes;
			ArrayList<CFGNodeImpl> currentExcEndNodes = this.currentExcEndNodes;

			finalExcEndNodes.addAll(currentExcEndNodes);

			while (!tail.isEmpty()) {
				head = tail.head;
				head.accept(this);
				finalExcEndNodes.addAll(this.currentExcEndNodes);
				tail = tail.tail;
			}
			this.currentStartNodes = currentStartNodes;
			finalExcEndNodes.addAll(this.currentExcEndNodes);
			this.currentExcEndNodes = finalExcEndNodes;
		} //else throw new Error("block should not be empty");
	}

	public void visitParallelStatements(List<? extends JCTree> statements) {
		ArrayList<CFGNodeImpl> finalExcEndNodes = new ArrayList<CFGNodeImpl>();
		ArrayList<CFGNodeImpl> finalEndNodes = new ArrayList<CFGNodeImpl>();
		JCTree head = statements.head;
		if (head != null) {
			List<? extends JCTree> tail = statements.tail;
			head.accept(this);
			ArrayList<CFGNodeImpl> currentStartNodes = this.currentStartNodes;
			ArrayList<CFGNodeImpl> currentEndNodes = this.currentEndNodes;
			ArrayList<CFGNodeImpl> currentExcEndNodes = this.currentExcEndNodes;

			finalExcEndNodes.addAll(currentExcEndNodes);
			finalEndNodes.addAll(currentEndNodes);

			while (!tail.isEmpty()) {
				head = tail.head;
				head.accept(this);
				finalExcEndNodes.addAll(this.currentExcEndNodes);
				finalEndNodes.addAll(this.currentEndNodes);
				tail = tail.tail;
			}
			this.currentStartNodes = currentStartNodes;
			finalExcEndNodes.addAll(this.currentExcEndNodes);
			this.currentEndNodes = finalEndNodes;
			this.currentExcEndNodes = finalExcEndNodes;
		} //else throw new Error("block should not be empty");
	}

	public static ArrayList<CFGNodeImpl> getBreaks(ArrayList<CFGNodeImpl> nodes) {
		ArrayList<CFGNodeImpl> results = new ArrayList<CFGNodeImpl>();
		for (CFGNodeImpl node : nodes) {
			if (node.tree instanceof JCBreak) {
				JCBreak jcb = (JCBreak)node.tree;

				if (jcb.label==null || jcb.target==null)
					results.add(node);
				else 
					throw new Error("jcb.label="+jcb.label+"\tjcb.target="+jcb.target);
			}
		}
		return results;
	}
}