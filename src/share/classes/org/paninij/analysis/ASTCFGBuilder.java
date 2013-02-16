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

import java.util.ArrayList;

import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;

import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeScanner;

/* This class combines the work of finding the header/tail/exit nodes of an AST
 * i.e. CFGHeadTailNodesBuilder.java, and connecting the nodes together, i.e.,
 * ASTNodeConector.java. */
public class ASTCFGBuilder extends TreeScanner {
	private int id = 0;
	private ArrayList<JCTree> currentStartNodes, currentEndNodes,
	currentExitNodes;

	private static ArrayList<JCTree> emptyList = new ArrayList<JCTree>(0);

	public void connectNodes(JCMethodDecl m, CFG cfg) {
		scan(m.body);
	}

	public void visitTopLevel(JCCompilationUnit tree)    { Assert.error(); }
	public void visitImport(JCImport tree)               { Assert.error(); }
	public void visitLetExpr(LetExpr tree)               { Assert.error(); }
	public void visitAssert(JCAssert tree)               { Assert.error(); }
	public void visitAnnotation(JCAnnotation tree)       { Assert.error(); }
	public void visitModifiers(JCModifiers tree)         { Assert.error(); }
	public void visitErroneous(JCErroneous tree)         { Assert.error(); }

	public void visitTypeIdent(JCPrimitiveTypeTree tree) { Assert.error(); }
	public void visitTypeArray(JCArrayTypeTree tree)     { Assert.error(); }
	public void visitTypeApply(JCTypeApply tree)         { Assert.error(); }
	public void visitTypeUnion(JCTypeUnion tree)         { Assert.error(); }
	public void visitTypeParameter(JCTypeParameter tree) { Assert.error(); }
	public void visitWildcard(JCWildcard tree)           { Assert.error(); }
	public void visitTypeBoundKind(TypeBoundKind tree)   { Assert.error(); }

	public void visitSkip(JCSkip tree)                   { /* do nothing */ }
	public void visitLabelled(JCLabeledStatement tree)   { /* do nothing */ }
	
	public void visitIdent(JCIdent tree)                 { singleton(tree); }
	public void visitLiteral(JCLiteral tree)             { singleton(tree); }    

	public void visitClassDef(JCClassDecl tree) {
		singleton(tree);

		for (JCTree def : tree.defs) { def.accept(this); }
	}

	public void visitMethodDef(JCMethodDecl tree) {
		JCBlock body = tree.body;
		if (body != null) { body.accept(this); }
	}

	public void visitVarDef(JCVariableDecl tree) {
		JCExpression init = tree.init;

		// fill the start/end/exit nodes
		if (init != null) {
			init.accept(this);
		} else {
			currentStartNodes = new ArrayList<JCTree>(1);
			currentStartNodes.add(tree);
		}

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		currentExitNodes = emptyList;

		addNode(tree);

		// connect the nodes
		if (init != null) {
			connectToEndNodesOf(tree, init);
		}
	}

	public void visitBlock(JCBlock tree) {
		List<JCStatement> stats = tree.stats;
		JCStatement head = stats.head;

		// fill the start/end/exit nodes
		if (head == null) { // cases where the block is empty.
			singleton(tree);
		} else { // head != null
			visitStatements(stats);

			addNode(tree);

			// connect the nodes
			visitList(stats);
		}
	}

	public void visitDoLoop(JCDoWhileLoop tree) {
		JCStatement body = tree.body;
		JCExpression cond = tree.cond;

		// fill the start/end/exit nodes
		ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();
		ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>();
		body.accept(this);

		ArrayList<JCTree> bodyStartNodes =
			new ArrayList<JCTree>(currentStartNodes);
		ArrayList<JCTree> bodyEndNodes =
			new ArrayList<JCTree>(currentEndNodes);
		ArrayList<JCTree> bodyExcEndNodes =
			new ArrayList<JCTree>(currentExitNodes);
		ArrayList<JCTree> breaks =
			getBreaks(bodyExcEndNodes);
		bodyEndNodes.addAll(breaks);
		bodyExcEndNodes.removeAll(breaks);

		cond.accept(this);
		finalEndNodes.addAll(bodyEndNodes);
		finalEndNodes.addAll(currentEndNodes);
		finalExcEndNodes.addAll(bodyExcEndNodes);
		finalExcEndNodes.addAll(currentExitNodes);

		currentStartNodes = bodyStartNodes;
		currentEndNodes = finalEndNodes;
		currentExitNodes = finalExcEndNodes;

		addNode(tree);

		// connect the nodes
		connectStartNodesToEndNodesOf(cond, body);
		connectStartNodesToEndNodesOf(body, cond);
		connectStartNodesToContinuesOf(tree, body);
	}

	public void visitWhileLoop(JCWhileLoop tree) {
		JCExpression cond = tree.cond;
		JCStatement body = tree.body;

		// fill the start/end/exit nodes
		cond.accept(this);
		ArrayList<JCTree> condStartNodes = currentStartNodes;
		ArrayList<JCTree> condEndNodes = currentEndNodes;
		ArrayList<JCTree> condExcEndNodes = currentExitNodes;

		body.accept(this);
		ArrayList<JCTree> bodyEndNodes =
			new ArrayList<JCTree>(currentEndNodes);
		ArrayList<JCTree> bodyExcEndNodes =
			new ArrayList<JCTree>(currentExitNodes);

		ArrayList<JCTree> breaks = getBreaks(bodyExcEndNodes);
		bodyEndNodes.addAll(breaks);
		bodyExcEndNodes.removeAll(breaks);

		ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();
		ArrayList<JCTree> finalExitNodes = new ArrayList<JCTree>();
		finalEndNodes.addAll(bodyEndNodes);
		finalEndNodes.addAll(condEndNodes);
		finalExitNodes.addAll(bodyExcEndNodes);
		finalExitNodes.addAll(condExcEndNodes);

		currentStartNodes = condStartNodes;
		currentEndNodes = finalEndNodes;
		currentExitNodes = finalExitNodes;

		addNode(tree);

		// connect the nodes
		connectStartNodesToEndNodesOf(cond, body);
		connectStartNodesToEndNodesOf(body, cond);
		connectStartNodesToContinuesOf(tree, body);
	}

	public void visitForLoop(JCForLoop tree) {
		List<JCStatement> init = tree.init;
		JCExpression cond = tree.cond;
		List<JCExpressionStatement> step = tree.step;
		JCStatement body = tree.body;

		if (init.isEmpty()) {
			ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();

			if (cond != null) {
				// fill the start/end/exit nodes
				cond.accept(this);
				finalEndNodes.addAll(currentEndNodes);

				ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
				body.accept(this);

				ArrayList<JCTree> currentEndNodes =
					new ArrayList<JCTree>(this.currentEndNodes);
				ArrayList<JCTree> currentExcEndNodes =
					new ArrayList<JCTree>(this.currentExitNodes);

				ArrayList<JCTree> breaks = getBreaks(currentExcEndNodes);
				currentEndNodes.addAll(breaks);
				currentExcEndNodes.removeAll(breaks);

				finalEndNodes.addAll(currentEndNodes);

				if (!step.isEmpty()) {
					visitStatements(tree.step);
					finalEndNodes.addAll(this.currentEndNodes);
				}

				this.currentStartNodes = currentStartNodes;
				this.currentEndNodes = finalEndNodes;
				this.currentExitNodes = currentExcEndNodes;

				addNode(tree);

				// connect the nodes
				connectStartNodesToEndNodesOf(body, cond);
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

				addNode(tree);
			}
		} else { /* !init.isEmpty() */
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

			addNode(tree);

			// connect the nodes
			JCTree lastStatement = visitList(tree.init);
			if (cond != null) {
				connectStartNodesToEndNodesOf(cond, lastStatement);
			} else {
				connectStartNodesToEndNodesOf(body, lastStatement);
			}
		}

		// connect the nodes
		JCTree nextStartNodeTree = null;

		if (step.isEmpty()) {
			if (cond != null) { nextStartNodeTree = cond;
			} else { nextStartNodeTree = body; }
		} else {
			nextStartNodeTree = step.head;

			JCTree lastStatement = visitList(step);
			if (cond != null) {
				connectStartNodesToEndNodesOf(cond, lastStatement);
			} else { connectStartNodesToEndNodesOf(body, lastStatement); }
		}

		connectStartNodesToEndNodesOf(nextStartNodeTree, body);
		connectStartNodesToContinuesOf(cond, body);
	}

	public void visitForeachLoop(JCEnhancedForLoop tree) {
		JCExpression expr = tree.expr;
		JCStatement body = tree.body;

		// fill the start/end/exit nodes
		expr.accept(this);
		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;

		body.accept(this);
		this.currentStartNodes = currentStartNodes;
		this.currentEndNodes = new ArrayList<JCTree>(currentEndNodes);
		this.currentEndNodes.add(tree);

		addNode(tree);

		// connect the nodes
		connectToEndNodesOf(tree, expr);
		connectToStartNodesOf(tree, expr);
		connectStartNodesToContinuesOf(tree, body);
	}

	/* used by visitSwitch and visitCase only, which visit the single node then
	 *  the subsequent list. */
	public void switchAndCase(JCTree single, List<? extends JCTree> list) {
		// single.accept(this);

		if (list.head != null) {
			// list.head.accept(this);
			connectStartNodesToEndNodesOf(list.head, single);
			JCTree prev = list.head;            
			for (JCTree tree : list.tail) {
				// tree.accept(this);
				connectStartNodesToEndNodesOf(tree, prev);
				prev = tree;
			}
		}
	}

	public void visitSwitch(JCSwitch tree) {
		JCExpression selector = tree.selector;
		List<JCCase> cases = tree.cases;

		// fill the start/end/exit nodes
		selector.accept(this);
		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
		ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();
		finalEndNodes.addAll(currentEndNodes);

		visitStatements(cases);

		this.currentStartNodes = currentStartNodes;
		ArrayList<JCTree> currentEndNodes =
			new ArrayList<JCTree>(this.currentEndNodes);
		ArrayList<JCTree> currentExcEndNodes =
			new ArrayList<JCTree>(this.currentExitNodes);

		ArrayList<JCTree> breaks = getBreaks(currentExcEndNodes);
		currentEndNodes.addAll(breaks);
		currentExcEndNodes.removeAll(breaks);
		finalEndNodes.addAll(currentEndNodes);

		this.currentStartNodes = currentStartNodes;
		this.currentEndNodes = finalEndNodes;
		this.currentExitNodes = currentExcEndNodes;

		addNode(tree);

		// connect the nodes
		switchAndCase(selector, cases);
	}

	public void visitCase(JCCase tree) {
		JCExpression pat = tree.pat;
		List<JCStatement> stats = tree.stats;

		// fill the start/end/exit nodes
		pat.accept(this);
		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;

		visitStatements(stats);

		this.currentStartNodes = currentStartNodes;

		addNode(tree);

		// connect the nodes
		switchAndCase(pat, stats);
	}

	public void visitSynchronized(JCSynchronized tree) {
		JCExpression lock = tree.lock;
		JCBlock body = tree.body;

		// fill the start/end/exit nodes
		lock.accept(this);
		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;

		body.accept(this);
		this.currentStartNodes = currentStartNodes;

		addNode(tree);

		// connect the nodes
		connectStartNodesToEndNodesOf(body, lock);
	}

	public void visitTry(JCTry tree) {
		JCBlock body = tree.body;
		List<JCCatch> catchers = tree.catchers;
		JCBlock finalizer = tree.finalizer;

		// fill the start/end/exit nodes
		body.accept(this);
		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;

		ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();
		ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>();
		finalEndNodes.addAll(currentEndNodes);
		finalExcEndNodes.addAll(currentExitNodes);

		if (catchers.isEmpty()) {
			if (finalizer != null) {
				finalizer.accept(this);
			}

			this.currentStartNodes = currentStartNodes;
			addNode(tree);
		} else {
			visitParallelStatements(catchers);

			if (finalizer != null) {
				finalizer.accept(this);
			}

			this.currentStartNodes = currentStartNodes;
			finalEndNodes.addAll(currentEndNodes);
			currentEndNodes = finalEndNodes;
			finalExcEndNodes.addAll(currentExitNodes);
			currentExitNodes = finalExcEndNodes;

			addNode(tree);

			// connect the nodes
			for (JCCatch c : catchers) {
				connectStartNodesToEndNodesOf(c, body);

				if (finalizer != null) {
					connectStartNodesToEndNodesOf(finalizer, c);
				}
			}
		}

		// connect the nodes
		if (finalizer != null) {
			connectStartNodesToEndNodesOf(finalizer, body);
		}
	}

	public void visitCatch(JCCatch tree) {
		JCVariableDecl param = tree.param;
		JCBlock body = tree.body;

		// fill the start/end/exit nodes
		param.accept(this);
		ArrayList<JCTree> finalStartNodes = currentStartNodes;
		ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>();
		finalExcEndNodes.addAll(currentExitNodes);

		body.accept(this);

		currentStartNodes = finalStartNodes;
		addNode(tree);

		// connect the nodes
		connectStartNodesToEndNodesOf(tree.body, tree.param);
	}

	public void visitConditional(JCConditional tree) {
		JCExpression cond = tree.cond;
		JCExpression truepart = tree.truepart;
		JCExpression falsepart = tree.falsepart;

		// fill the start/end/exit nodes
		cond.accept(this);
		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;

		ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();
		ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>();
		truepart.accept(this);
		finalEndNodes.addAll(this.currentEndNodes);
		finalExcEndNodes.addAll(this.currentExitNodes);

		falsepart.accept(this);
		finalEndNodes.addAll(this.currentEndNodes);
		finalExcEndNodes.addAll(this.currentExitNodes);

		this.currentStartNodes = currentStartNodes;
		this.currentEndNodes = finalEndNodes;
		this.currentExitNodes = finalExcEndNodes;

		addNode(tree);

		// connect the nodes
		connectStartNodesToEndNodesOf(truepart, cond);
		connectStartNodesToEndNodesOf(falsepart, cond);
	}

	public void visitIf(JCIf tree) {
		JCExpression cond = tree.cond;
		JCStatement thenpart = tree.thenpart;
		JCStatement elsepart = tree.elsepart;

		// fill the start/end/exit nodes
		cond.accept(this);
		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
		ArrayList<JCTree> currentEndNodes = this.currentEndNodes;

		ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();
		ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>();

		thenpart.accept(this);
		finalEndNodes.addAll(this.currentEndNodes);
		finalExcEndNodes.addAll(currentExitNodes);

		if (elsepart != null) {
			elsepart.accept(this);
			finalEndNodes.addAll(this.currentEndNodes);
			finalExcEndNodes.addAll(currentExitNodes);
		} else {
			finalEndNodes.addAll(currentEndNodes);
		}

		this.currentStartNodes = currentStartNodes;
		this.currentEndNodes = finalEndNodes;
		this.currentExitNodes = finalExcEndNodes;

		addNode(tree);

		// connect the nodes

		connectStartNodesToEndNodesOf(thenpart, cond);

		if (elsepart != null) {
			connectStartNodesToEndNodesOf(elsepart, cond);
		}
	}

	public void visitExec(JCExpressionStatement tree) {
		tree.expr.accept(this);
		addNode(tree);
	}

	public void visitBreak(JCBreak tree) {
		// fill the start/end/exit nodes
		singleton(tree);

		// connect the nodes
		if (tree.target != null) {
			connectToEndNodesOf(tree, tree.target);
		}
	}

	public void visitContinue(JCContinue tree) {
		// fill the start/end/exit nodes
		singleton(tree);

		// connect the nodes
		if (tree.target != null) {
			connectToEndNodesOf(tree, tree.target);
		}
	}

	public void visitReturn(JCReturn tree) {
		// fill the start/end/exit nodes
		JCExpression expr = tree.expr;

		if (expr != null) {
			expr.accept(this);
		} else {
			currentStartNodes = new ArrayList<JCTree>(1);
			currentStartNodes.add(tree);
		}

		currentEndNodes = emptyList;
		currentExitNodes = new ArrayList<JCTree>(1);
		currentExitNodes.add(tree);

		addNode(tree);

		// connect the nodes
		if (expr != null) {
			connectToEndNodesOf(tree, expr);
		}
	}

	public void visitThrow(JCThrow tree) {
		// fill the start/end/exit nodes
		JCExpression expr = tree.expr;

		expr.accept(this);
		currentEndNodes = emptyList;
		currentExitNodes = new ArrayList<JCTree>(1);
		currentExitNodes.add(tree);
		addNode(tree);

		// connect the nodes
		// if (expr != null) {
		connectToEndNodesOf(tree, expr);
		// }
	}

	public void visitApply(JCMethodInvocation tree) {
		JCExpression meth = tree.meth;
		List<JCExpression> args = tree.args;

		// fill the start/end/exit nodes
		meth.accept(this);

		ArrayList<JCTree> startNodes = currentStartNodes;

		if (!args.isEmpty()) {
			visitStatements(args);
		}

		currentStartNodes = startNodes;

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);
		currentExitNodes = emptyList;
		addNode(tree);

		// connect the nodes
		if (!args.isEmpty()) {
			JCTree lastArg = visitList(args);
			connectStartNodesToEndNodesOf(lastArg, meth);
			connectToEndNodesOf(tree, lastArg);
		} else { connectToEndNodesOf(tree, meth); }
	}

	public void visitNewClass(JCNewClass tree) {
		List<JCExpression> args = tree.args;

		// fill the start/end/exit nodes
		if (args.isEmpty()) {
			currentStartNodes = new ArrayList<JCTree>(1);
			currentStartNodes.add(tree);
		} else {
			visitStatements(args);
		}

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);
		currentExitNodes = emptyList;
		addNode(tree);

		// connect the nodes
		if (!args.isEmpty()) {
			JCTree lastArg = visitList(args);
			connectToEndNodesOf(tree, lastArg);
		}
	}

	public void visitNewArray(JCNewArray tree) {
		List<JCExpression> dims = tree.dims;
		List<JCExpression> elems = tree.elems;

		// fill the start/end/exit nodes
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

		// connect the nodes
		if (!dims.isEmpty()) {
			JCTree lastDimension = visitList(dims);

			if (elems != null) {
				if (!elems.isEmpty()) {
					JCTree lastElement = visitList(elems);

					connectStartNodesToEndNodesOf(
							elems.head, lastDimension);
					connectToEndNodesOf(tree, lastElement);
				} else {
					connectToEndNodesOf(tree, lastDimension);
				}
			} else { 
				connectToEndNodesOf(tree, lastDimension);
			}
		}
	}

	public void visitParens(JCParens tree) {
		tree.expr.accept(this);
		addNode(tree);
	}

	public void visitAssign(JCAssign tree) {
		JCExpression lhs = tree.lhs;
		JCExpression rhs = tree.rhs;

		// fill the start/end/exit nodes
		lhs.accept(this);

		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
		rhs.accept(this);

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		this.currentStartNodes = currentStartNodes;
		addNode(tree);

		// connect the nodes
		connectStartNodesToEndNodesOf(rhs, lhs);
		connectToEndNodesOf(tree, rhs);
	}

	public void visitAssignop(JCAssignOp tree) {
		JCExpression lhs = tree.lhs;
		JCExpression rhs = tree.rhs;

		// fill the start/end/exit nodes
		lhs.accept(this);
		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
		rhs.accept(this);

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		this.currentStartNodes = currentStartNodes;
		addNode(tree);

		// connect the nodes
		connectStartNodesToEndNodesOf(rhs, lhs);
		connectToEndNodesOf(tree, rhs);
	}

	public void visitUnary(JCUnary tree) {
		// fill the start/end/exit nodes
		tree.arg.accept(this);

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		addNode(tree);

		// connect the nodes
		connectToEndNodesOf(tree, tree.arg);
	}

	public void visitBinary(JCBinary tree) {
		JCExpression lhs = tree.lhs;
		JCExpression rhs = tree.rhs;

		// fill the start/end/exit nodes
		lhs.accept(this);
		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
		rhs.accept(this);

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		this.currentStartNodes = currentStartNodes;
		addNode(tree);

		// connect the nodes
		connectStartNodesToEndNodesOf(rhs, lhs);
		connectToEndNodesOf(tree, rhs);
	}

	public void visitTypeCast(JCTypeCast tree) {
		JCExpression expr = tree.expr;

		// fill the start/end/exit nodes
		expr.accept(this);

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		addNode(tree);

		// connect the nodes
		connectToEndNodesOf(tree, expr);
	}

	public void visitTypeTest(JCInstanceOf tree) {
		JCExpression expr = tree.expr;

		// fill the start/end/exit nodes
		expr.accept(this);

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		addNode(tree);

		// connect the nodes
		connectToEndNodesOf(tree, expr);
	}

	public void visitIndexed(JCArrayAccess tree) {
		JCExpression indexed = tree.indexed;
		JCExpression index = tree.index;

		// fill the start/end/exit nodes
		indexed.accept(this);

		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
		ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>();
		finalExcEndNodes.addAll(currentExitNodes);

		index.accept(this);

		finalExcEndNodes.addAll(currentExitNodes);

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		currentExitNodes = finalExcEndNodes;
		this.currentStartNodes = currentStartNodes;

		addNode(tree);

		// connect the nodes
		connectStartNodesToEndNodesOf(index, indexed);
		connectToEndNodesOf(tree, index);
	}

	public void visitSelect(JCFieldAccess tree) {
		JCExpression selected = tree.selected;

		// fill the start/end/exit nodes
		selected.accept(this);
		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		currentExitNodes = emptyList;

		addNode(tree);

		// connect the nodes
		connectToEndNodesOf(tree, selected);
	}

	public JCTree visitList(List<? extends JCTree> trees) {
		JCTree last = null;
		if (trees.head != null) {
			// trees.head.accept(this);
			last = trees.head;

			for (JCTree tree : trees.tail) {
				// tree.accept(this);
				connectStartNodesToEndNodesOf(tree, last);
				last = tree;
			}
		}
		return last;
	}

	// This method is called to lazy init the successors and predecessors or a
	// JCTree node.
	private static void init(JCTree tree) {
		if (tree.predecessors == null) {
			tree.predecessors = new ArrayList<JCTree>();
		}

		if (tree.successors == null) {
			tree.successors = new ArrayList<JCTree>();
		}
	}

	private static void connectToEndNodesOf(JCTree start, JCTree end) {
		for (JCTree endNode : start.endNodes) {
			if (end.successors != null) {
				end.successors.add(endNode);
			}
			if (endNode.predecessors != null) {
				endNode.predecessors.add(end);
			}
		}
	}

	private static void connectStartNodesToEndNodesOf(
			JCTree start, JCTree end) {
		for (JCTree endNode : end.endNodes) {
			for (JCTree startNode : start.startNodes) {
				endNode.successors.add(startNode);
				startNode.predecessors.add(endNode);
			}
		}
	}

	private static void connectStartNodesToContinuesOf(
			JCTree start, JCTree end) {
		for (JCTree endNode : end.exitNodes) {
			if (endNode instanceof JCBreak) {
				throw new Error("should not reach JCBreak");
			} else if (endNode instanceof JCContinue) {
				endNode.successors.addAll(start.startNodes);
				for (JCTree startNode : start.startNodes) {
					startNode.predecessors.add(endNode);
				}
			} else if (endNode instanceof JCReturn) {
			} else if (endNode instanceof JCThrow) {
			} else throw new Error("this shouldn't happen");
		}
	}

	private static void connectToStartNodesOf(JCTree start, JCTree end) {
		for (JCTree startNode : end.startNodes) {
			startNode.predecessors.add(start);
			start.successors.add(startNode);
		}
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
		} // else throw new Error("block should not be empty");
	}

	private void singleton(JCTree tree) {
		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);
		currentExitNodes = emptyList;

		currentStartNodes = new ArrayList<JCTree>(1);
		currentStartNodes.add(tree);

		addNode(tree);
	}

	private void addNode(JCTree tree) {
		tree.id = id++;
		tree.startNodes = currentStartNodes;
		tree.endNodes = currentEndNodes;
		tree.exitNodes = currentExitNodes;

		init(tree);
	}

	private static ArrayList<JCTree> getBreaks(ArrayList<JCTree> nodes) {
		ArrayList<JCTree> results = new ArrayList<JCTree>();
		for (JCTree tree : nodes) {
			if (tree instanceof JCBreak) {
				JCBreak jcb = (JCBreak)tree;

				if (jcb.label == null || jcb.target == null)
					results.add(tree);
				else
					throw new Error("jcb.label = " + jcb.label +
							"\tjcb.target = " + jcb.target);
			}
		}
		return results;
	}

	private void visitParallelStatements(List<? extends JCTree> statements) {
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
}