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

public class ASTNodeConnector extends TreeScanner {
	public void connectNodes(JCMethodDecl m, CFG cfg) {
		scan(m.body);
	}

	public void visitTopLevel(JCCompilationUnit tree)    { Assert.error(); }
	public void visitImport(JCImport tree)               { Assert.error(); }
	public void visitMethodDef(JCMethodDecl tree)        { Assert.error(); }
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

	public void visitClassDef(JCClassDecl tree)          { /* do nothing */ }
	public void visitSkip(JCSkip tree)                   { /* do nothing */ }
	public void visitLabelled(JCLabeledStatement tree)   { /* do nothing */ }
	public void visitIdent(JCIdent tree)                 { /* do nothing */ }
	public void visitLiteral(JCLiteral tree)             { /* do nothing */ }    

	public void visitVarDef(JCVariableDecl tree) {
		JCExpression init = tree.init;
		if (init != null) {
			init.accept(this);
			connectToEndNodesOf(tree, init);
		}
	}

	public void visitBlock(JCBlock tree) {
		if (tree.stats.head != null) {
			visitList(tree.stats);
		}
	}

	public void visitDoLoop(JCDoWhileLoop tree) {
		tree.body.accept(this);
		tree.cond.accept(this);

		connectStartNodesToEndNodesOf(tree.cond, tree.body);
		connectStartNodesToEndNodesOf(tree.body, tree.cond);
		connectStartNodesToContinuesOf(tree, tree.body);
	}

	public void visitWhileLoop(JCWhileLoop tree) {
		JCExpression cond = tree.cond;
		JCStatement body = tree.body;

		cond.accept(this);
		body.accept(this);

		connectStartNodesToEndNodesOf(cond, body);
		connectStartNodesToEndNodesOf(body, cond);
		connectStartNodesToContinuesOf(tree, tree.body);
	}

	public void visitForLoop(JCForLoop tree) {
		JCTree lastStatement = visitList(tree.init);

		JCExpression cond = tree.cond;

		if (cond != null) {
			cond.accept(this);
		}

		if (lastStatement != null && cond != null) {
			connectStartNodesToEndNodesOf(cond, lastStatement);
		}

		JCStatement body = tree.body;
		body.accept(this);

		if (cond != null) {
			connectStartNodesToEndNodesOf(body, cond);
		} else if (lastStatement != null) {
			connectStartNodesToEndNodesOf(body, lastStatement);
		}

		JCTree nextStartNodeTree = null;

		List<JCExpressionStatement> step = tree.step;
		if (step.isEmpty()) {
			if (cond != null) { nextStartNodeTree = cond;
			} else { nextStartNodeTree = body; }
		} else {
			nextStartNodeTree = step.head;

			lastStatement = visitList(step);
			if (cond != null) {
				connectStartNodesToEndNodesOf(cond, lastStatement);
			} else { connectStartNodesToEndNodesOf(body, lastStatement); }
		}

		connectStartNodesToEndNodesOf(nextStartNodeTree, body);
		connectStartNodesToContinuesOf(cond, body);
	}

	public void visitForeachLoop(JCEnhancedForLoop tree) {
		tree.expr.accept(this);
		connectToEndNodesOf(tree, tree.expr);

		tree.body.accept(this);

		connectToStartNodesOf(tree, tree.expr);

		connectStartNodesToContinuesOf(tree, tree.body);
	}

	/* used by visitSwitch and visitCase only, which visit the single node then
	 *  the subsequent list. */
	public void switchAndCase(JCTree single, List<? extends JCTree> list) {
		single.accept(this);

		if (list.head != null) {
			list.head.accept(this);
			connectStartNodesToEndNodesOf(list.head, single);
			JCTree prev = list.head;            
			for (JCTree tree : list.tail) {
				tree.accept(this);
				connectStartNodesToEndNodesOf(tree, prev);
				prev = tree;
			}
		}
	}

	public void visitSwitch(JCSwitch tree) {
		switchAndCase(tree.selector, tree.cases);
	}

	public void visitCase(JCCase tree) {
		switchAndCase(tree.pat, tree.stats);
	}

	public void visitSynchronized(JCSynchronized tree) {
		tree.lock.accept(this);
		tree.body.accept(this);
		connectStartNodesToEndNodesOf(tree.body, tree.lock);
	}

	public void visitTry(JCTry tree) {
		JCBlock body = tree.body;
		List<JCCatch> catchers = tree.catchers;
		JCBlock finalizer = tree.finalizer;
		body.accept(this);

		if (finalizer != null) { finalizer.accept(this); }

		if (catchers.isEmpty()) {
			if (finalizer != null) 
				connectStartNodesToEndNodesOf(finalizer, body);
		} else {
			for (JCCatch c : tree.catchers) {
				c.accept(this);
				connectStartNodesToEndNodesOf(c, body);

				if (finalizer != null)
					connectStartNodesToEndNodesOf(finalizer, c);
			}
			if (finalizer != null) 
				connectStartNodesToEndNodesOf(finalizer, body);
		}
	}

	public void visitCatch(JCCatch tree) {
		tree.param.accept(this);
		tree.body.accept(this);

		connectStartNodesToEndNodesOf(tree.body, tree.param);
	}

	public void visitConditional(JCConditional tree) {
		tree.cond.accept(this);
		tree.truepart.accept(this);
		tree.falsepart.accept(this);

		connectStartNodesToEndNodesOf(tree.truepart, tree.cond);
		connectStartNodesToEndNodesOf(tree.falsepart, tree.cond);
	}

	public void visitIf(JCIf tree) {
		tree.cond.accept(this);
		tree.thenpart.accept(this);

		connectStartNodesToEndNodesOf(tree.thenpart, tree.cond);

		if (tree.elsepart != null) {
			tree.elsepart.accept(this);
			connectStartNodesToEndNodesOf(tree.elsepart, tree.cond);
		}
	}

	public void visitExec(JCExpressionStatement tree) {
		tree.expr.accept(this);
	}

	public void visitBreak(JCBreak tree) {
		if (tree.target != null) {
			connectToEndNodesOf(tree, tree.target);
		}
	}

	public void visitContinue(JCContinue tree) {
		if (tree.target != null) {
			connectToEndNodesOf(tree, tree.target);
		}
	}

	public void visitReturn(JCReturn tree) {
		if (tree.expr != null) {
			tree.expr.accept(this);
			connectToEndNodesOf(tree, tree.expr);
		}
	}

	public void visitThrow(JCThrow tree) {
		if (tree.expr != null) {
			tree.expr.accept(this);
			connectToEndNodesOf(tree, tree.expr);
		}
	}

	public void visitApply(JCMethodInvocation tree) {
		tree.meth.accept(this);
		if (!tree.args.isEmpty()) {
			JCTree lastArg = visitList(tree.args);
			connectStartNodesToEndNodesOf(lastArg, tree.meth);
			connectToEndNodesOf(tree, lastArg);
		} else { connectToEndNodesOf(tree, tree.meth); }
	}

	public void visitNewClass(JCNewClass tree) {
		if (!tree.args.isEmpty()) {
			JCTree lastArg = visitList(tree.args);
			connectToEndNodesOf(tree, lastArg);
		}
	}

	public void visitNewArray(JCNewArray tree) {
		List<JCExpression> dims = tree.dims;
		List<JCExpression> elems = tree.elems;

		if (!dims.isEmpty()) {
			JCTree lastDimension = visitList(dims);

			if (elems != null) {
				if (!elems.isEmpty()) {
					JCTree lastElement = visitList(elems);

					connectStartNodesToEndNodesOf(
							elems.head, lastDimension);
					connectToEndNodesOf(tree, lastElement);
				} else
					connectToEndNodesOf(tree, lastDimension);
			} else 
				connectToEndNodesOf(tree, lastDimension);
		}
	}

	public void visitParens(JCParens tree) {
		tree.expr.accept(this);
	}

	public void visitAssign(JCAssign tree) {
		tree.lhs.accept(this);
		tree.rhs.accept(this);

		connectStartNodesToEndNodesOf(tree.rhs, tree.lhs);
		connectToEndNodesOf(tree, tree.rhs);
	}

	public void visitAssignop(JCAssignOp tree) {
		tree.lhs.accept(this);
		tree.rhs.accept(this);

		connectStartNodesToEndNodesOf(tree.rhs, tree.lhs);
		connectToEndNodesOf(tree, tree.rhs);
	}

	public void visitBinary(JCBinary tree) {
		tree.lhs.accept(this);
		tree.rhs.accept(this);

		connectStartNodesToEndNodesOf(tree.rhs, tree.lhs);
		connectToEndNodesOf(tree, tree.rhs);
	}

	public void visitUnary(JCUnary tree) {
		tree.arg.accept(this);
		connectToEndNodesOf(tree, tree.arg);
	}

	public void visitTypeCast(JCTypeCast tree) {
		tree.expr.accept(this);
		connectToEndNodesOf(tree, tree.expr);
	}

	public void visitTypeTest(JCInstanceOf tree) {
		tree.expr.accept(this);
		connectToEndNodesOf(tree, tree.expr);
	}

	public void visitIndexed(JCArrayAccess tree) {
		tree.indexed.accept(this);
		tree.index.accept(this);

		connectStartNodesToEndNodesOf(tree.index, tree.indexed);
		connectToEndNodesOf(tree, tree.index);
	}

	public void visitSelect(JCFieldAccess tree) {
		JCExpression selected = tree.selected;
		selected.accept(this);
		connectToEndNodesOf(tree, selected);
	}

	public JCTree visitList(List<? extends JCTree> trees) {
		JCTree last = null;
		if (trees.head != null) {
			trees.head.accept(this);
			last = trees.head;

			for (JCTree tree : trees.tail) {
				tree.accept(this);
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
		init(end);

		for (JCTree endNode : start.endNodes) {
			init(endNode);
			endNode.successors.add(end);
			end.successors.add(endNode);
		}
	}

	private static void connectStartNodesToEndNodesOf(
			JCTree start, JCTree end) {
		for (JCTree endNode : end.endNodes) {
			init(endNode);
			for (JCTree startNode : start.startNodes) {
				init(startNode);
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
				init(endNode);
				endNode.successors.addAll(start.startNodes);
				for (JCTree startNode : start.startNodes) {
					init(startNode);
					startNode.predecessors.add(endNode);
				}
			} else if (endNode instanceof JCReturn) {
			} else if (endNode instanceof JCThrow) {
			} else throw new Error("this shouldn't happen");
		}
	}

	private static void connectToStartNodesOf(JCTree start, JCTree end) {
		init(start);
		for (JCTree startNode : end.startNodes) {
			init(startNode);
			startNode.predecessors.add(start);
			start.successors.add(startNode);
		}
	}
}