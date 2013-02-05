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
 * Contributor(s): Rex Fernando and Yuheng Long
 */

package org.paninij.analysis;

import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeScanner;

import java.util.ArrayList;

/* This class finds out the head/tail nodes for a AST.
 * For example, the head tree of and if statement is
 * the condition expression, because it is first executed. */
public class CFGHeadTailNodesBuilder extends TreeScanner {
	private ArrayList<JCTree> currentStartNodes, currentEndNodes,
		currentExitNodes;

	public static ArrayList<JCTree> emptyList = new ArrayList<JCTree>(0);

	public void buildNodes(JCMethodDecl m) {
		scan(m.body);
	}

	public void visitMethodDef(JCMethodDecl that)        { Assert.error(); }

	public void visitClassDef(JCClassDecl that) { singleton(that); }
	public void visitSkip(JCSkip that) { singleton(that); }
	public void visitBreak(JCBreak that) { singleton(that); }
	public void visitContinue(JCContinue that) { singleton(that); }
	public void visitLiteral(JCLiteral that) { singleton(that); }
	public void visitIdent(JCIdent that) { singleton(that); }
	public void visitLabelled(JCLabeledStatement that) { singleton(that); }

	public void visitTopLevel(JCCompilationUnit that)    { Assert.error(); }
	public void visitImport(JCImport that)               { Assert.error(); }
	public void visitProcDef(JCProcDecl that)            { Assert.error(); }

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

	public void visitVarDef(JCVariableDecl tree) {
		JCExpression init = tree.init;
		if (init != null) {
			tree.init.accept(this);
		} else {
			currentStartNodes = new ArrayList<JCTree>(1);
			currentStartNodes.add(tree);
		}

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		currentExitNodes = emptyList;

		addNode(tree);
	}

	public void visitBlock(JCBlock tree) {
		List<JCStatement> stats = tree.stats;
		JCStatement head = stats.head;

		if (head == null) { // cases where the block is empty.
			singleton(tree);
		} else {
			visitStatements(stats);

			addNode(tree);
		}
	}

	public void visitDoLoop(JCDoWhileLoop tree) {
		ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();
		ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>();
		tree.body.accept(this);

		ArrayList<JCTree> bodyStartNodes =
			new ArrayList<JCTree>(this.currentStartNodes);
		ArrayList<JCTree> bodyEndNodes =
			new ArrayList<JCTree>(this.currentEndNodes);
		ArrayList<JCTree> bodyExcEndNodes =
			new ArrayList<JCTree>(this.currentExitNodes);
		ArrayList<JCTree> breaks =
			getBreaks(bodyExcEndNodes);
		bodyEndNodes.addAll(breaks);
		bodyExcEndNodes.removeAll(breaks);

		tree.cond.accept(this);
		finalEndNodes.addAll(bodyEndNodes);
		finalEndNodes.addAll(this.currentEndNodes);
		finalExcEndNodes.addAll(bodyExcEndNodes);
		finalExcEndNodes.addAll(this.currentExitNodes);

		this.currentStartNodes = bodyStartNodes;
		this.currentEndNodes = finalEndNodes;
		this.currentExitNodes = finalExcEndNodes;

		addNode(tree);
	}

	public void visitWhileLoop(JCWhileLoop tree) {
		tree.cond.accept(this);
		ArrayList<JCTree> condStartNodes = this.currentStartNodes;
		ArrayList<JCTree> condEndNodes = this.currentEndNodes;
		ArrayList<JCTree> condExcEndNodes = this.currentExitNodes;

		tree.body.accept(this);
		ArrayList<JCTree> bodyEndNodes =
			new ArrayList<JCTree>(this.currentEndNodes);
		ArrayList<JCTree> bodyExcEndNodes =
			new ArrayList<JCTree>(this.currentExitNodes);

		ArrayList<JCTree> breaks = getBreaks(bodyExcEndNodes);
		bodyEndNodes.addAll(breaks);
		bodyExcEndNodes.removeAll(breaks);

		ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();
		ArrayList<JCTree> finalExitNodes = new ArrayList<JCTree>();
		finalEndNodes.addAll(bodyEndNodes);
		finalEndNodes.addAll(condEndNodes);
		finalExitNodes.addAll(bodyExcEndNodes);
		finalExitNodes.addAll(condExcEndNodes);

		this.currentStartNodes = condStartNodes;
		this.currentEndNodes = finalEndNodes;
		this.currentExitNodes = finalExitNodes;

		addNode(tree);
	}

	public void visitForLoop(JCForLoop tree) {
		if (tree.init.isEmpty()) {
			ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();

			if (tree.cond != null) {
				tree.cond.accept(this);
				finalEndNodes.addAll(currentEndNodes);

				ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
				tree.body.accept(this);

				ArrayList<JCTree> currentEndNodes =
					new ArrayList<JCTree>(this.currentEndNodes);
				ArrayList<JCTree> currentExcEndNodes =
					new ArrayList<JCTree>(this.currentExitNodes);

				ArrayList<JCTree> breaks = getBreaks(currentExcEndNodes);
				currentEndNodes.addAll(breaks);
				currentExcEndNodes.removeAll(breaks);

				finalEndNodes.addAll(currentEndNodes);

				if (!tree.step.isEmpty()) {
					visitStatements(tree.step);
					finalEndNodes.addAll(this.currentEndNodes);
				}

				this.currentStartNodes = currentStartNodes;
				this.currentEndNodes = finalEndNodes;
				this.currentExitNodes = currentExcEndNodes;
			} else { /* tree.cond == null, condition is empty. */
				tree.body.accept(this);

				ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
				ArrayList<JCTree> currentEndNodes =
					new ArrayList<JCTree>(this.currentEndNodes);
				ArrayList<JCTree> currentExcEndNodes =
					new ArrayList<JCTree>(this.currentExitNodes);
	
				ArrayList<JCTree> breaks = getBreaks(currentExcEndNodes);
				currentEndNodes.addAll(breaks);
				currentExcEndNodes.removeAll(breaks);
				finalEndNodes.addAll(currentEndNodes);
	
				if (!tree.step.isEmpty()) {
					visitStatements(tree.step);
					finalEndNodes.addAll(this.currentEndNodes);
				}

				this.currentStartNodes = currentStartNodes;
				this.currentEndNodes = finalEndNodes;
				this.currentExitNodes = currentExcEndNodes;
			}
		} else {/*!init.isEmpty()*/
			ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();

			visitStatements(tree.init);
			ArrayList<JCTree> currentStartNodes = this.currentStartNodes;

			if (tree.cond != null) {
				tree.cond.accept(this);
			}

			tree.body.accept(this);
			ArrayList<JCTree> currentEndNodes =
				new ArrayList<JCTree>(this.currentEndNodes);
			ArrayList<JCTree> currentExcEndNodes =
				new ArrayList<JCTree>(this.currentExitNodes);
			ArrayList<JCTree> breaks = getBreaks(currentExcEndNodes);
			currentEndNodes.addAll(breaks);
			currentExcEndNodes.removeAll(breaks);
			finalEndNodes.addAll(currentEndNodes);

			if (!tree.step.isEmpty()) {
				visitStatements(tree.step);
				finalEndNodes.addAll(this.currentEndNodes);
			}

			this.currentStartNodes = currentStartNodes;
			this.currentEndNodes = finalEndNodes;
			this.currentExitNodes = currentExcEndNodes;
		}
		addNode(tree);
	}

	public void visitForeachLoop(JCEnhancedForLoop tree) {
		tree.expr.accept(this);
		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;

		tree.body.accept(this);
		this.currentStartNodes = currentStartNodes;
		this.currentEndNodes = new ArrayList<JCTree>(currentEndNodes);
		this.currentEndNodes.add(tree);

		addNode(tree);
	}

	public void visitSwitch(JCSwitch tree) {
		tree.selector.accept(this);
		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
		ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();
		finalEndNodes.addAll(currentEndNodes);

		visitStatements(tree.cases);

		this.currentStartNodes = currentStartNodes;
		ArrayList<JCTree> currentEndNodes =
			new ArrayList<JCTree>(this.currentEndNodes);
		ArrayList<JCTree> currentExcEndNodes =
			new ArrayList<JCTree>(this.currentExitNodes);

		ArrayList<JCTree> breaks = getBreaks(currentExcEndNodes);
		currentEndNodes.addAll(breaks); currentExcEndNodes.removeAll(breaks);
		finalEndNodes.addAll(currentEndNodes);

		this.currentStartNodes = currentStartNodes;
		this.currentEndNodes = finalEndNodes;
		this.currentExitNodes = currentExcEndNodes;

		addNode(tree);
	}

	public void visitCase(JCCase tree) {
		tree.pat.accept(this);
		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;

		visitStatements(tree.stats);

		this.currentStartNodes = currentStartNodes;

		addNode(tree);
	}

	public void visitSynchronized(JCSynchronized tree) {
		tree.lock.accept(this);
		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;

		tree.body.accept(this);
		this.currentStartNodes = currentStartNodes;

		addNode(tree);
	}

	public void visitTry(JCTry tree) {
		tree.body.accept(this);
		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;

		ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();
		ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>();
		finalEndNodes.addAll(currentEndNodes);
		finalExcEndNodes.addAll(currentExitNodes);

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
			} else {
				this.currentStartNodes = currentStartNodes;
				finalEndNodes.addAll(currentEndNodes);
				currentEndNodes = finalEndNodes;
				finalExcEndNodes.addAll(currentExitNodes);
				currentExitNodes = finalExcEndNodes;
			}
		}
		addNode(tree);
	}

	public void visitCatch(JCCatch tree) {
		tree.param.accept(this);
		ArrayList<JCTree> finalStartNodes = currentStartNodes;
		ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>();
		finalExcEndNodes.addAll(currentExitNodes);

		tree.body.accept(this);

		currentStartNodes = finalStartNodes;
		addNode(tree);
	}

	public void visitConditional(JCConditional tree) {
		tree.cond.accept(this);
		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;

		ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();
		ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>();
		tree.truepart.accept(this);
		finalEndNodes.addAll(this.currentEndNodes);
		finalExcEndNodes.addAll(this.currentExitNodes);

		tree.falsepart.accept(this);
		finalEndNodes.addAll(this.currentEndNodes);
		finalExcEndNodes.addAll(this.currentExitNodes);

		this.currentStartNodes = currentStartNodes;
		this.currentEndNodes = finalEndNodes;
		this.currentExitNodes = finalExcEndNodes;

		addNode(tree);
	}

	public void visitIf(JCIf tree) {
		tree.cond.accept(this);
		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
		ArrayList<JCTree> currentEndNodes = this.currentEndNodes;

		ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();
		ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>();

		tree.thenpart.accept(this);
		finalEndNodes.addAll(this.currentEndNodes);
		finalExcEndNodes.addAll(currentExitNodes);

		if (tree.elsepart != null) {
			tree.elsepart.accept(this);
			finalEndNodes.addAll(this.currentEndNodes);
			finalExcEndNodes.addAll(currentExitNodes);
		} else {
			finalEndNodes.addAll(currentEndNodes);
		}

		this.currentStartNodes = currentStartNodes;
		this.currentEndNodes = finalEndNodes;
		this.currentExitNodes = finalExcEndNodes;

		addNode(tree);
	}

	public void visitExec(JCExpressionStatement tree) {
		tree.expr.accept(this);

		addNode(tree);
	}

	public void visitReturn(JCReturn tree) {
		if (tree.expr != null) {
			tree.expr.accept(this);
		} else {
			currentStartNodes = new ArrayList<JCTree>(1);
			currentStartNodes.add(tree);
		}

		currentEndNodes = emptyList;
		currentExitNodes = new ArrayList<JCTree>(1);
		currentExitNodes.add(tree);

		addNode(tree);
	}

	public void visitThrow(JCThrow tree) {
		tree.expr.accept(this);
		currentEndNodes = emptyList;
		currentExitNodes = new ArrayList<JCTree>(1);
		currentExitNodes.add(tree);
		addNode(tree);
	}

	public void visitApply(JCMethodInvocation tree) {
		tree.meth.accept(this);

		ArrayList<JCTree> startNodes = currentStartNodes;

		if (!tree.args.isEmpty()) {
			visitStatements(tree.args);
		}

		currentStartNodes = startNodes;

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);
		currentExitNodes = emptyList;
		addNode(tree);
	}

	public void visitNewClass(JCNewClass tree) {
		if (tree.args.isEmpty()) {
			currentStartNodes = new ArrayList<JCTree>(1);
			currentStartNodes.add(tree);
		} else {
			visitStatements(tree.args);
		}

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);
		currentExitNodes = emptyList;
		addNode(tree);
	}

	public void visitNewArray(JCNewArray tree) {
		List<JCExpression> dims = tree.dims;
		List<JCExpression> elems = tree.elems;
		ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>();
		ArrayList<JCTree> finalStartNodes = null;
		if (!dims.isEmpty()) {
			visitStatements(dims);
			finalExcEndNodes.addAll(currentExitNodes);
			finalStartNodes = currentStartNodes;
		}

		if (elems != null) {
			if (!elems.isEmpty()) {
				visitStatements(elems);
				finalExcEndNodes.addAll(currentExitNodes);
			}
		}

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		currentExitNodes = finalExcEndNodes;

		if (finalStartNodes == null) {
			currentStartNodes = new ArrayList<JCTree>(1);
			currentStartNodes.add(tree);
		} else { currentStartNodes = finalStartNodes; }

		addNode(tree);
	}

	public void visitParens(JCParens tree) {
		tree.expr.accept(this);
		addNode(tree);
	}

	public void visitAssign(JCAssign tree) {
		tree.lhs.accept(this);

		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
		tree.rhs.accept(this);

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		this.currentStartNodes = currentStartNodes;
		addNode(tree);
	}

	public void visitAssignop(JCAssignOp tree) {
		tree.lhs.accept(this);
		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
		tree.rhs.accept(this);

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		this.currentStartNodes = currentStartNodes;
		addNode(tree);
	}

	public void visitUnary(JCUnary tree) {
		tree.arg.accept(this);

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		addNode(tree);
	}

	public void visitBinary(JCBinary tree) {
		tree.lhs.accept(this);
		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
		tree.rhs.accept(this);

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		this.currentStartNodes = currentStartNodes;
		addNode(tree);
	}

	public void visitTypeCast(JCTypeCast tree) {
		tree.expr.accept(this);

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		addNode(tree);
	}

	public void visitTypeTest(JCInstanceOf tree) {
		tree.expr.accept(this);

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		addNode(tree);
	}

	public void visitIndexed(JCArrayAccess tree) {
		tree.indexed.accept(this);

		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
		ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>();
		finalExcEndNodes.addAll(currentExitNodes);

		tree.index.accept(this);

		finalExcEndNodes.addAll(currentExitNodes);

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		currentExitNodes = finalExcEndNodes;
		this.currentStartNodes = currentStartNodes;

		addNode(tree);
	}

	public void visitSelect(JCFieldAccess tree) {
		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		currentExitNodes = emptyList;

		addNode(tree);
	}

	public void visitProcApply(JCProcInvocation tree) {
		visitApply(tree);
	}

	public void visitFree(JCFree tree) {
		scan(tree.exp);
	}

	private void addNode(JCTree tree) {
		tree.startNodes = currentStartNodes;
		tree.endNodes = currentEndNodes;
		tree.exitNodes = currentExitNodes;
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

	public void visitTree(JCTree tree) {
		Assert.error();
	}

	public void singleton(JCTree tree) {
		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);
		currentExitNodes = emptyList;

		currentStartNodes = new ArrayList<JCTree>(1);
		currentStartNodes.add(tree);

		addNode(tree);
	}

	private void visitStatements(List<? extends JCTree> statements) {
		ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>();
		JCTree head = statements.head;
		if (head != null) {
			List<? extends JCTree> tail = statements.tail;
			head.accept(this);
			ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
			ArrayList<JCTree> currentExcEndNodes = this.currentExitNodes;

			finalExcEndNodes.addAll(currentExcEndNodes);

			while (!tail.isEmpty()) {
				head = tail.head;
				head.accept(this);
				finalExcEndNodes.addAll(this.currentExitNodes);
				tail = tail.tail;
			}
			this.currentStartNodes = currentStartNodes;
			finalExcEndNodes.addAll(this.currentExitNodes);
			this.currentExitNodes = finalExcEndNodes;
		} //else throw new Error("block should not be empty");
	}

	public void visitParallelStatements(List<? extends JCTree> statements) {
		ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>();
		ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();
		JCTree head = statements.head;
		if (head != null) {
			List<? extends JCTree> tail = statements.tail;
			head.accept(this);
			ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
			ArrayList<JCTree> currentEndNodes = this.currentEndNodes;
			ArrayList<JCTree> currentExcEndNodes = this.currentExitNodes;

			finalExcEndNodes.addAll(currentExcEndNodes);
			finalEndNodes.addAll(currentEndNodes);

			while (!tail.isEmpty()) {
				head = tail.head;
				head.accept(this);
				finalExcEndNodes.addAll(this.currentExitNodes);
				finalEndNodes.addAll(this.currentEndNodes);
				tail = tail.tail;
			}
			this.currentStartNodes = currentStartNodes;
			finalExcEndNodes.addAll(this.currentExitNodes);
			this.currentEndNodes = finalEndNodes;
			this.currentExitNodes = finalExcEndNodes;
		} //else throw new Error("block should not be empty");
	}

	public static ArrayList<JCTree> getBreaks(ArrayList<JCTree> nodes) {
		ArrayList<JCTree> results = new ArrayList<JCTree>();
		for (JCTree tree : nodes) {
			if (tree instanceof JCBreak) {
				JCBreak jcb = (JCBreak)tree;

				if (jcb.label==null || jcb.target==null)
					results.add(tree);
				else 
					throw new Error("jcb.label="+jcb.label+"\tjcb.target="+jcb.target);
			}
		}
		return results;
	}
}