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

import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeScanner;

public class CFGNodeConnector extends TreeScanner {
	private CFG cfg;

	public void connectNodes(JCMethodDecl m, CFG cfg) {
		this.cfg = cfg;

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

	public void visitClassDef(JCClassDecl tree)          {/*do nothing*/}
	public void visitSkip(JCSkip tree)                   {/*do nothing*/}
	public void visitLabelled(JCLabeledStatement tree)   {/*do nothing*/}
	public void visitIdent(JCIdent tree)                 {/*do nothing*/}
	public void visitLiteral(JCLiteral tree)             {/*do nothing*/}    

	public void visitVarDef(JCVariableDecl tree) {
		JCExpression init = tree.init;
		if (init != null) {
			init.accept(this);
			cfg.nodeForTree(tree).connectToEndNodesOf(cfg.nodeForTree(init));
		}
	}

	public void visitBlock(JCBlock tree) {
		if (tree.stats.head != null) {
			visitList(tree.stats);
		} //else throw new Error("block should not be empty");
	}

	public void visitDoLoop(JCDoWhileLoop tree) {
		tree.body.accept(this);
		tree.cond.accept(this);

		cfg.nodeForTree(tree.cond).connectStartNodesToEndNodesOf(
				cfg.nodeForTree(tree.body));
		cfg.nodeForTree(tree.body).connectStartNodesToEndNodesOf(
				cfg.nodeForTree(tree.cond));
		cfg.nodeForTree(tree).connectStartNodesToContinuesOf(
				cfg.nodeForTree(tree.body));
	}

	public void visitWhileLoop(JCWhileLoop tree) {
		JCExpression cond = tree.cond;
		JCStatement body = tree.body;

		cond.accept(this);
		body.accept(this);

		cfg.nodeForTree(cond).connectStartNodesToEndNodesOf(
				cfg.nodeForTree(body));
		cfg.nodeForTree(body).connectStartNodesToEndNodesOf(
				cfg.nodeForTree(cond));
		cfg.nodeForTree(tree).connectStartNodesToContinuesOf(
				cfg.nodeForTree(tree.body));
	}

	public void visitForLoop(JCForLoop tree) {
		//		List<JCStatement> init = tree.init;
		JCTree lastStatement = visitList(tree.init);

		tree.cond.accept(this);
		if (lastStatement != null && tree.cond != null) {
			cfg.nodeForTree(tree.cond).connectStartNodesToEndNodesOf(
					cfg.nodeForTree(lastStatement));
		}

		tree.body.accept(this);
		if (tree.cond != null) {
			cfg.nodeForTree(tree.body).connectStartNodesToEndNodesOf(
					cfg.nodeForTree(tree.cond));
		} else if (lastStatement != null) {
			cfg.nodeForTree(tree.body).connectStartNodesToEndNodesOf(
					cfg.nodeForTree(lastStatement));
		}

		JCTree nextStartNodeTree = null;

		if (tree.step.isEmpty()) {
			if (tree.cond != null) {
				nextStartNodeTree = tree.cond;
			} else {
				nextStartNodeTree = tree.body;
			}
		} else {
			nextStartNodeTree = tree.step.head;

			lastStatement = visitList(tree.step);
			if (tree.cond != null) 
				cfg.nodeForTree(tree.cond).connectStartNodesToEndNodesOf(
						cfg.nodeForTree(lastStatement));
			else cfg.nodeForTree(tree.body).connectStartNodesToEndNodesOf(
					cfg.nodeForTree(lastStatement));
		}

		cfg.nodeForTree(lastStatement).connectStartNodesToEndNodesOf(
				cfg.nodeForTree(tree.body));
		cfg.nodeForTree(lastStatement).connectStartNodesToContinuesOf(
				cfg.nodeForTree(tree.body));
	}

	public void visitForeachLoop(JCEnhancedForLoop tree) {
		tree.expr.accept(this);
		cfg.nodeForTree(tree).connectToEndNodesOf(cfg.nodeForTree(tree.expr));

		tree.body.accept(this);

		cfg.nodeForTree(tree).connectToStartNodesOf(cfg.nodeForTree(tree.expr));

		cfg.nodeForTree(tree).connectStartNodesToContinuesOf(
				cfg.nodeForTree(tree.body));
	}

	/* used by visitSwitch and visitCase only, which visit the single node then
	 *  the subsequent list. */
	public void switchAndCase(JCTree single, List<? extends JCTree> list) {
		single.accept(this);

		if (list.head != null) {
			list.head.accept(this);
			cfg.nodeForTree(list.head).connectStartNodesToEndNodesOf(
					cfg.nodeForTree(single));
			JCTree prev = list.head;            
			for (JCTree tree : list.tail) {
				tree.accept(this);
				cfg.nodeForTree(tree).connectStartNodesToEndNodesOf(
						cfg.nodeForTree(prev));
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
		cfg.nodeForTree(tree.body).connectStartNodesToEndNodesOf(
				cfg.nodeForTree(tree.lock));
	}

	public void visitTry(JCTry tree) {
		JCBlock body = tree.body;
		List<JCCatch> catchers = tree.catchers;
		JCBlock finalizer = tree.finalizer;
		tree.body.accept(this);

		if (tree.finalizer != null) { tree.finalizer.accept(this); }

		if (tree.catchers.isEmpty()) {
			if (finalizer != null) 
				cfg.nodeForTree(finalizer).connectStartNodesToEndNodesOf(
						cfg.nodeForTree(body));
		} else {
			for (JCCatch c : tree.catchers) {
				c.accept(this);
				cfg.nodeForTree(c).connectStartNodesToEndNodesOf(
						cfg.nodeForTree(body));

				if (finalizer != null)
					cfg.nodeForTree(finalizer).connectStartNodesToEndNodesOf(
							cfg.nodeForTree(c));
			}
			if (finalizer != null) 
				cfg.nodeForTree(finalizer).connectStartNodesToEndNodesOf(
						cfg.nodeForTree(body));
		}
	}

	public void visitCatch(JCCatch tree) {
		tree.param.accept(this);
		tree.body.accept(this);

		cfg.nodeForTree(tree.body).connectStartNodesToEndNodesOf(
				cfg.nodeForTree(tree.param));
	}

	public void visitConditional(JCConditional tree) {
		tree.cond.accept(this);
		tree.truepart.accept(this);
		tree.falsepart.accept(this);

		cfg.nodeForTree(tree.truepart).connectStartNodesToEndNodesOf(
				cfg.nodeForTree(tree.cond));
		cfg.nodeForTree(tree.falsepart).connectStartNodesToEndNodesOf(
				cfg.nodeForTree(tree.cond));
	}

	public void visitIf(JCIf tree) {
		tree.cond.accept(this);
		tree.thenpart.accept(this);

		cfg.nodeForTree(tree.thenpart).connectStartNodesToEndNodesOf(
				cfg.nodeForTree(tree.cond));

		if (tree.elsepart != null) {
			tree.elsepart.accept(this);
			cfg.nodeForTree(tree.elsepart).connectStartNodesToEndNodesOf(
					cfg.nodeForTree(tree.cond));
		}
	}

	public void visitExec(JCExpressionStatement tree) {
		tree.expr.accept(this);
	}

	public void visitBreak(JCBreak tree) {
		if (tree.target != null) {
			cfg.nodeForTree(tree).connectToEndNodesOf(
					cfg.nodeForTree(tree.target));
		}
	}

	public void visitContinue(JCContinue tree) {
		if (tree.target != null) {
			cfg.nodeForTree(tree).connectToEndNodesOf(
					cfg.nodeForTree(tree.target));
		}
	}

	public void visitReturn(JCReturn tree) {
		if (tree.expr != null) {
			tree.expr.accept(this);
			cfg.nodeForTree(tree).connectToEndNodesOf(
					cfg.nodeForTree(tree.expr));
		}
	}

	public void visitThrow(JCThrow tree) {
		if (tree.expr != null) {
			tree.expr.accept(this);
			cfg.nodeForTree(tree).connectToEndNodesOf(
					cfg.nodeForTree(tree.expr));
		}
	}

	public void visitApply(JCMethodInvocation tree) {
		tree.meth.accept(this);
		if (!tree.args.isEmpty()) {
			JCTree lastArg = visitList(tree.args);
			cfg.nodeForTree(lastArg).connectStartNodesToEndNodesOf(
					cfg.nodeForTree(tree.meth));
			cfg.nodeForTree(tree).connectToEndNodesOf(cfg.nodeForTree(lastArg));
		} else 
			cfg.nodeForTree(tree).connectToEndNodesOf(cfg.nodeForTree(tree.meth));
	}

	public void visitNewClass(JCNewClass tree) {
		if (!tree.args.isEmpty()) {
			JCTree lastArg = visitList(tree.args);
			cfg.nodeForTree(tree).connectToEndNodesOf(cfg.nodeForTree(lastArg));
		}
	}

	public void visitNewArray(JCNewArray tree) {
		List<JCExpression> dims = tree.dims;
		List<JCExpression> elems = tree.elems;

		if (!tree.dims.isEmpty()) {
			JCTree lastDimension = visitList(tree.dims);

			if (tree.elems != null) {
				if (!tree.elems.isEmpty()) {
					JCTree lastElement = visitList(tree.elems);

					cfg.nodeForTree(tree.elems.head).
					connectStartNodesToEndNodesOf(
							cfg.nodeForTree(lastDimension));
					cfg.nodeForTree(tree).connectToEndNodesOf(
							cfg.nodeForTree(lastElement));
				} else
					cfg.nodeForTree(tree).connectToEndNodesOf(
							cfg.nodeForTree(lastDimension));
			} else 
				cfg.nodeForTree(tree).connectToEndNodesOf(
						cfg.nodeForTree(lastDimension));
		}
	}

	public void visitParens(JCParens tree) {
		tree.expr.accept(this);
	}

	public void visitAssign(JCAssign tree) {
		tree.lhs.accept(this);
		tree.rhs.accept(this);

		cfg.nodeForTree(tree.rhs).connectStartNodesToEndNodesOf(
				cfg.nodeForTree(tree.lhs));
		cfg.nodeForTree(tree).connectToEndNodesOf(
				cfg.nodeForTree(tree.rhs));
	}

	public void visitAssignop(JCAssignOp tree) {
		tree.lhs.accept(this);
		tree.rhs.accept(this);

		cfg.nodeForTree(tree.rhs).connectStartNodesToEndNodesOf(
				cfg.nodeForTree(tree.lhs));
		cfg.nodeForTree(tree).connectToEndNodesOf(
				cfg.nodeForTree(tree.rhs));
	}

	public void visitBinary(JCBinary tree) {
		tree.lhs.accept(this);
		tree.rhs.accept(this);

		cfg.nodeForTree(tree.rhs).connectStartNodesToEndNodesOf(
				cfg.nodeForTree(tree.lhs));
		cfg.nodeForTree(tree).connectToEndNodesOf(
				cfg.nodeForTree(tree.rhs));
	}

	public void visitUnary(JCUnary tree) {
		tree.arg.accept(this);
		cfg.nodeForTree(tree).connectToEndNodesOf(cfg.nodeForTree(tree.arg));
	}

	public void visitTypeCast(JCTypeCast tree) {
		tree.expr.accept(this);
		cfg.nodeForTree(tree).connectToEndNodesOf(cfg.nodeForTree(tree.expr));
	}

	public void visitTypeTest(JCInstanceOf tree) {
		tree.expr.accept(this);
		cfg.nodeForTree(tree).connectToEndNodesOf(cfg.nodeForTree(tree.expr));
	}

	public void visitIndexed(JCArrayAccess tree) {
		tree.indexed.accept(this);
		tree.index.accept(this);

		cfg.nodeForTree(tree.index).connectStartNodesToEndNodesOf(
				cfg.nodeForTree(tree.indexed));
		cfg.nodeForTree(tree).connectToEndNodesOf(cfg.nodeForTree(tree.index));
	}

	public void visitSelect(JCFieldAccess tree) {
		JCExpression selected = tree.selected;
		tree.selected.accept(this);
		cfg.nodeForTree(tree).connectToEndNodesOf(
				cfg.nodeForTree(tree.selected));
	}

	public JCTree visitList(List<? extends JCTree> trees) {
		JCTree last = null;
		if (trees.head != null) {
			trees.head.accept(this);
			last = trees.head;

			for (JCTree tree : trees.tail) {
				tree.accept(this);
				cfg.nodeForTree(tree).connectStartNodesToEndNodesOf(
						cfg.nodeForTree(last));
				last = tree;
			}
		}
		return last;
	}
}