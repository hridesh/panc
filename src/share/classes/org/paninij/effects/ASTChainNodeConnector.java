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

package org.paninij.effects;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.jvm.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;

import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.code.Type.*;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.SimpleTreeVisitor;

import java.util.ArrayList;


public class ASTChainNodeConnector extends TreeScanner {
    private JCMethodDecl m;
    private ASTChain chain;
    private ArrayList<ASTChainNode> currentStartNodes, currentEndNodes, currentExcEndNodes;
	private final ArrayList<ASTChainNode> emptyList = new ArrayList<ASTChainNode>(0);

    public void connectNodes(JCMethodDecl m, ASTChain chain) {
        this.m = m;
        this.chain = chain;
        
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
		if(init != null) {
			init.accept(this);
//			Util.chainListItem(astnodes, astChainMapping, init, tree);
            chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(init));
		}
	}

	public void visitBlock(JCBlock tree) {
		if(tree.stats.head != null) {
            visitList(tree.stats);
		} //else throw new Error("block should not be empty");
	}

	public void visitDoLoop(JCDoWhileLoop tree) {
		tree.body.accept(this);
		tree.cond.accept(this);

        chain.nodeForTree(tree.cond).connectStartNodesToEndNodesOf(chain.nodeForTree(tree.body));
        chain.nodeForTree(tree.body).connectStartNodesToEndNodesOf(chain.nodeForTree(tree.cond));
        chain.nodeForTree(tree).connectStartNodesToContinuesOf(chain.nodeForTree(tree.body));
	}

	public void visitWhileLoop(JCWhileLoop tree) {
		JCExpression cond = tree.cond;
		JCStatement body = tree.body;

		cond.accept(this);
		body.accept(this);

        chain.nodeForTree(cond).connectStartNodesToEndNodesOf(chain.nodeForTree(body));
        chain.nodeForTree(body).connectStartNodesToEndNodesOf(chain.nodeForTree(cond));
        chain.nodeForTree(tree).connectStartNodesToContinuesOf(chain.nodeForTree(tree.body));
	}

	public void visitForLoop(JCForLoop tree) {
//		List<JCStatement> init = tree.init;
//		JCTree lastStmt = Util.handleList(init, astnodes, astChainMapping, this);
        JCTree lastStatement = visitList(tree.init);

		tree.cond.accept(this);
		if(lastStatement != null && tree.cond != null) {
            chain.nodeForTree(tree.cond).connectStartNodesToEndNodesOf(chain.nodeForTree(lastStatement));
		}

		tree.body.accept(this);
		if(tree.cond != null) {
            chain.nodeForTree(tree.body).connectStartNodesToEndNodesOf(chain.nodeForTree(tree.cond));
		} else if(lastStatement != null) {
            chain.nodeForTree(tree.body).connectStartNodesToEndNodesOf(chain.nodeForTree(lastStatement));
		}

		JCTree nextStartNodeTree = null;

		if(tree.step.isEmpty()) {
			if(tree.cond != null) {
				nextStartNodeTree = tree.cond;
			} else {
				nextStartNodeTree = tree.body;
			}
		} else {
			nextStartNodeTree = tree.step.head;

            lastStatement = visitList(tree.step);
			if(tree.cond != null) 
                chain.nodeForTree(tree.cond).connectStartNodesToEndNodesOf(chain.nodeForTree(lastStatement));
			else chain.nodeForTree(tree.body).connectStartNodesToEndNodesOf(chain.nodeForTree(lastStatement));
        }

        chain.nodeForTree(lastStatement).connectStartNodesToEndNodesOf(chain.nodeForTree(tree.body));
        chain.nodeForTree(lastStatement).connectStartNodesToContinuesOf(chain.nodeForTree(tree.body));
	}

	public void visitForeachLoop(JCEnhancedForLoop tree) {
		tree.expr.accept(this);
        chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(tree.expr));

		tree.body.accept(this);

		//Util.chainItemList(astnodes, astChainMapping, tree, body);
        chain.nodeForTree(tree).connectToStartNodesOf(chain.nodeForTree(tree.expr));
        
        chain.nodeForTree(tree).connectStartNodesToContinuesOf(chain.nodeForTree(tree.body));
	}

	/* used by visitSwitch and visitCase only, which visit the single node then the subsequent list. */
	public void switchAndCase(JCTree single, List<? extends JCTree> list) {
		single.accept(this);


		if(list.head != null) {
			list.head.accept(this);
            chain.nodeForTree(list.head).connectStartNodesToEndNodesOf(chain.nodeForTree(single));
            JCTree prev = list.head;            
			for(JCTree tree : list.tail) {
				tree.accept(this);
                chain.nodeForTree(tree).connectStartNodesToEndNodesOf(chain.nodeForTree(prev));
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
        chain.nodeForTree(tree.body).connectStartNodesToEndNodesOf(chain.nodeForTree(tree.lock));
	}

	public void visitTry(JCTry tree) {
		JCBlock body = tree.body;
		List<JCCatch> catchers = tree.catchers;
		JCBlock finalizer = tree.finalizer;
		tree.body.accept(this);

		if(tree.finalizer != null) { tree.finalizer.accept(this); }

		if(tree.catchers.isEmpty()) {
            if (finalizer != null) 
                chain.nodeForTree(finalizer).connectStartNodesToEndNodesOf(chain.nodeForTree(body));
		} else {
			for(JCCatch c : tree.catchers) {
				c.accept(this);
                chain.nodeForTree(c).connectStartNodesToEndNodesOf(chain.nodeForTree(body));

				if(finalizer != null)
                    chain.nodeForTree(finalizer).connectStartNodesToEndNodesOf(chain.nodeForTree(c));
			}
            if (finalizer != null) 
                chain.nodeForTree(finalizer).connectStartNodesToEndNodesOf(chain.nodeForTree(body));
        }
	}

	public void visitCatch(JCCatch tree) {
		tree.param.accept(this);
		tree.body.accept(this);

        chain.nodeForTree(tree.body).connectStartNodesToEndNodesOf(chain.nodeForTree(tree.param));
	}

	public void visitConditional(JCConditional tree) {
		tree.cond.accept(this);
		tree.truepart.accept(this);
		tree.falsepart.accept(this);

        chain.nodeForTree(tree.truepart).connectStartNodesToEndNodesOf(chain.nodeForTree(tree.cond));
        chain.nodeForTree(tree.falsepart).connectStartNodesToEndNodesOf(chain.nodeForTree(tree.cond));
	}

	public void visitIf(JCIf tree) {
		tree.cond.accept(this);
		tree.thenpart.accept(this);

        chain.nodeForTree(tree.thenpart).connectStartNodesToEndNodesOf(chain.nodeForTree(tree.cond));

		if(tree.elsepart != null) {
			tree.elsepart.accept(this);
            chain.nodeForTree(tree.elsepart).connectStartNodesToEndNodesOf(chain.nodeForTree(tree.cond));
		}
	}

	public void visitExec(JCExpressionStatement tree) {
		tree.expr.accept(this);
	}

	public void visitBreak(JCBreak tree) {
		if(tree.target != null) {
            chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(tree.target));
		}

	}

	public void visitContinue(JCContinue tree) {
		if(tree.target != null) {
            chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(tree.target));
		}
	}

	public void visitReturn(JCReturn tree) {
		if(tree.expr != null) {
			tree.expr.accept(this);
            chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(tree.expr));
		}
//		exists.add(astChainMapping.get(tree));
	}

	public void visitThrow(JCThrow tree) {
		if(tree.expr != null) {
			tree.expr.accept(this);
            chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(tree.expr));
		}
//		exists.add(astChainMapping.get(tree));
	}

	public void visitApply(JCMethodInvocation tree) {
		if(!tree.args.isEmpty()) {
            JCTree lastArg = visitList(tree.args);
            chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(lastArg));
		}
	}

	public void visitNewClass(JCNewClass tree) {
		if(!tree.args.isEmpty()) {
            JCTree lastArg = visitList(tree.args);
            chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(lastArg));
		}
	}

	public void visitNewArray(JCNewArray tree) {
		List<JCExpression> dims = tree.dims;
		List<JCExpression> elems = tree.elems;

		if(!tree.dims.isEmpty()) {
            JCTree lastDimension = visitList(tree.dims);
			
			if(tree.elems != null) {
				if(!tree.elems.isEmpty()) {
                    JCTree lastElement = visitList(tree.elems);

                    chain.nodeForTree(tree.elems.head).connectStartNodesToEndNodesOf(chain.nodeForTree(lastDimension));
                    chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(lastElement));
				} else
                    chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(lastDimension));
            } else 
                chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(lastDimension));
		}
	}

	public void visitParens(JCParens tree) {
		tree.expr.accept(this);
	}

	public void visitAssign(JCAssign tree) {
		tree.lhs.accept(this);
		tree.rhs.accept(this);

        chain.nodeForTree(tree.rhs).connectStartNodesToEndNodesOf(chain.nodeForTree(tree.lhs));
        chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(tree.rhs));
	}

	public void visitAssignop(JCAssignOp tree) {
		tree.lhs.accept(this);
		tree.rhs.accept(this);

        chain.nodeForTree(tree.rhs).connectStartNodesToEndNodesOf(chain.nodeForTree(tree.lhs));
        chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(tree.rhs));
	}

	public void visitBinary(JCBinary tree) {
		tree.lhs.accept(this);
		tree.rhs.accept(this);

        chain.nodeForTree(tree.rhs).connectStartNodesToEndNodesOf(chain.nodeForTree(tree.lhs));
        chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(tree.rhs));
	}

	public void visitUnary(JCUnary tree) {
		tree.arg.accept(this);
        chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(tree.arg));
	}

	public void visitTypeCast(JCTypeCast tree) {
		tree.expr.accept(this);
        chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(tree.expr));
	}

	public void visitTypeTest(JCInstanceOf tree) {
		tree.expr.accept(this);
        chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(tree.expr));
	}

	public void visitIndexed(JCArrayAccess tree) {
		tree.indexed.accept(this);
		tree.index.accept(this);

        chain.nodeForTree(tree.index).connectStartNodesToEndNodesOf(chain.nodeForTree(tree.indexed));
        chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(tree.index));
	}

	public void visitSelect(JCFieldAccess tree) {
		JCExpression selected = tree.selected;
		tree.selected.accept(this);
        chain.nodeForTree(tree).connectToEndNodesOf(chain.nodeForTree(tree.selected));
	}

    public JCTree visitList(List<? extends JCTree> trees) {
        JCTree last = null;
        if (trees.head != null) {
            trees.head.accept(this);
            last = trees.head;

            for (JCTree tree : trees.tail) {
                tree.accept(this);
                chain.nodeForTree(tree).connectStartNodesToEndNodesOf(chain.nodeForTree(last));
                last = tree;
            }
        }
        return last;
    }
}