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
 * Contributor(s): Rex Fernando, Yuheng Long and Ganesha Upadhyaya
 */

package org.paninij.analysis;

import java.util.ArrayList;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;

import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeScanner;

import org.paninij.effects.EffectInter;

public class ASTCFGBuilder extends TreeScanner {
	private int id = 0;
	private ArrayList<JCTree> currentStartNodes;
	private ArrayList<JCTree> currentEndNodes;
	private ArrayList<JCTree> currentExitNodes;
	private static ArrayList<JCTree> emptyList = new ArrayList<JCTree>(0);

	private EffectInter effectsBuilder;
	private TreeMaker make;

	public ASTCFGBuilder(TreeMaker make) {
		effectsBuilder = new EffectInter();
		this.make = make;
	}
	
	// methodCost
	private int methodCost = 0;
	private int loop = 0;
	// methodCost

	public void connectNodes(JCMethodDecl m, CFG cfg) {
		scan(m.body);
	}

	public void visitTopLevel(JCCompilationUnit tree) { Assert.error(); }
	public void visitImport(JCImport tree) { Assert.error(); }
	public void visitLetExpr(LetExpr tree) { Assert.error(); }
	public void visitAnnotation(JCAnnotation tree) { Assert.error(); }
	public void visitModifiers(JCModifiers tree) { Assert.error(); }
	public void visitErroneous(JCErroneous tree) { Assert.error(); }
	public void visitTypeApply(JCTypeApply tree) { Assert.error(); }
	public void visitTypeUnion(JCTypeUnion tree) { Assert.error(); }
	public void visitTypeParameter(JCTypeParameter tree) { Assert.error(); }
	public void visitWildcard(JCWildcard tree) { Assert.error(); }
	public void visitTypeBoundKind(TypeBoundKind tree) { Assert.error(); }

	public void visitTypeIdent(JCPrimitiveTypeTree tree) { singleton(tree); }
	public void visitIdent(JCIdent tree) { singleton(tree); }
	public void visitLiteral(JCLiteral tree) { singleton(tree); }
	// URL[].class
	public void visitTypeArray(JCArrayTypeTree tree) { singleton(tree); }
	// while (i != 0);
	public void visitSkip(JCSkip tree) { singleton(tree); }

	public void visitClassDef(JCClassDecl tree) {
		singleton(tree);

		for (JCTree def : tree.defs) {
			def.accept(this);
		}
	}

	public void visitLabelled(JCLabeledStatement tree) {
		JCStatement body = tree.body;

		assert (body != null);

		if (body != null) {
			body.accept(this);

			ArrayList<JCTree> finalEndNodes =
				new ArrayList<JCTree>(currentEndNodes);
			ArrayList<JCTree> predecessors = new ArrayList<JCTree>();

			// for breaks;
			ArrayList<JCTree> finalExitNodes =
				resolveBreaks(tree, finalEndNodes, currentExitNodes);

			// for continues;
			finalExitNodes =
				resolveContinues(tree, predecessors, finalExitNodes);

			currentExitNodes = finalExitNodes;
			currentEndNodes = finalEndNodes;
			currentStartNodes = new ArrayList<JCTree>(1);
			currentStartNodes.add(tree);

			addNode(tree);
			connectToStartNodesOf(tree, body);

			tree.predecessors.addAll(predecessors);
			for (JCTree endNode : predecessors) {
				endNode.successors.add(tree);
			}
		}
	}

	public void visitAssert(JCAssert tree) {
		JCExpression cond = tree.cond;

		// fill the start/end/exit nodes
		cond.accept(this);

		ArrayList<JCTree> currentStartNodes = this.currentStartNodes;

		JCExpression detail = tree.detail;
		if (detail != null) {
			detail.accept(this);

			this.currentStartNodes = currentStartNodes;

			ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>(1);
			ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>(1);
			
			finalEndNodes.add(tree);
			finalExcEndNodes.add(tree);

			this.currentEndNodes = finalEndNodes;
			this.currentExitNodes = finalExcEndNodes;
			addNode(tree);

			// connect the nodes
			connectStartNodesToEndNodesOf(detail, cond);
			connectToEndNodesOf(detail, tree);
		} else {
			ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>(1);
			ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>(1);
			
			finalEndNodes.add(tree);
			finalExcEndNodes.add(tree);

			this.currentEndNodes = finalEndNodes;
			this.currentExitNodes = finalExcEndNodes;
			addNode(tree);

			// connect the nodes
			connectToEndNodesOf(cond, tree);
		}
	}

	public ArrayList<JCTree> order;
	public void visitMethodDef(JCMethodDecl tree) {
		Symbol tree_sym = tree.sym;
		ClassSymbol cs = tree_sym.ownerCapsule();
        if (cs != null && cs.tree != null) {
		    Assert.checkNonNull(cs.capsule_info);

		    String tree_name = tree_sym.toString();
		    ClassSymbol encl = tree_sym.enclClass();
	        if ((encl != null && encl.capsule_info != null) &&
	        		AnalysisUtil.activeThread(cs, tree_name) ||
	        		AnalysisUtil.originalMethod(cs, tree, tree_name)) {
				ArrayList<JCTree> previous = order;
				order = new ArrayList<JCTree>();

				this.methodCost = 0; // reset methodCost
				JCBlock body = tree.body;
				// ensure that body is analyzed once and only once
				if (body != null && body.predecessors == null) {
					body.accept(this);
					tree.order = order;
					effectsBuilder.analysis(tree, cs, make);
				}
				tree.cost = methodCost;

				order = previous;
			}
		}
	}

	public void visitVarDef(JCVariableDecl tree) {
		JCExpression init = tree.init;

		// methodCost
		if (this.loop > 0) {
			methodCost += (Costs.iload * this.loop * 128); // TODO: loopBooster
			methodCost += (Costs.istore * this.loop * 128);
		} else {
			methodCost += Costs.iload;
			methodCost += Costs.istore;
		}
		// methodCost
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
			connectToEndNodesOf(init, tree);
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
		// methodCost
		this.loop++;
		// methodCost
		// fill the start/end/exit nodes
		ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();
		ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>();
		body.accept(this);

		ArrayList<JCTree> bodyStartNodes = new ArrayList<JCTree>(
				currentStartNodes);
		ArrayList<JCTree> bodyEndNodes = new ArrayList<JCTree>(currentEndNodes);
		ArrayList<JCTree> bodyExcEndNodes = new ArrayList<JCTree>(
				currentExitNodes);

		// for breaks;
		ArrayList<JCTree> tempBreakNodes = new ArrayList<JCTree>();
		ArrayList<JCTree> tempContinueNodes = new ArrayList<JCTree>();
		bodyExcEndNodes = resolveBreaks(tree, tempBreakNodes, bodyExcEndNodes);

		// for continues;
		bodyEndNodes.addAll(tempBreakNodes);
		bodyExcEndNodes = resolveContinues(tree, tempContinueNodes,
				bodyExcEndNodes);

		cond.accept(this);
		// finalEndNodes.addAll(bodyEndNodes);
		finalEndNodes.addAll(tempBreakNodes);
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
		// connectStartNodesToContinuesOf(tree, body);
		for (JCTree jct1 : tempContinueNodes) {
			jct1.successors.addAll(currentStartNodes);
			for (JCTree jct2 : currentStartNodes) {
				jct2.predecessors.add(jct1);
			}
		}

		// methodCost
		this.loop--;
		// methodCost
	}

	public void visitWhileLoop(JCWhileLoop tree) {
		JCExpression cond = tree.cond;
		JCStatement body = tree.body;
		// methodCost
		this.loop++;
		// methodCost
		// fill the start/end/exit nodes
		cond.accept(this);
		ArrayList<JCTree> condStartNodes = currentStartNodes;
		ArrayList<JCTree> condEndNodes = currentEndNodes;
		ArrayList<JCTree> condExcEndNodes = currentExitNodes;
		ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();

		body.accept(this);
		ArrayList<JCTree> bodyExcEndNodes = new ArrayList<JCTree>(
				currentExitNodes);

		// for breaks;
		ArrayList<JCTree> tempBreakNodes = new ArrayList<JCTree>();
		ArrayList<JCTree> tempContinueNodes = new ArrayList<JCTree>();
		bodyExcEndNodes = resolveBreaks(tree, tempBreakNodes, bodyExcEndNodes);

		// for continues;
		finalEndNodes.addAll(tempBreakNodes);
		bodyExcEndNodes = resolveContinues(tree, tempContinueNodes,
				bodyExcEndNodes);

		ArrayList<JCTree> finalExitNodes = new ArrayList<JCTree>();
		// finalEndNodes.addAll(bodyEndNodes);
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
		// connectStartNodesToContinuesOf(tree, body);
		for (JCTree jct1 : tempContinueNodes) {
			jct1.successors.addAll(condStartNodes);
			for (JCTree jct2 : condStartNodes) {
				jct2.predecessors.add(jct1);
			}
		}

		// methodCost
		this.loop--;
		// methodCost
	}

	public void visitForLoop(JCForLoop tree) {
		List<JCStatement> init = tree.init;
		JCExpression cond = tree.cond;
		List<JCExpressionStatement> step = tree.step;
		JCStatement body = tree.body;
		// methodCost
		this.loop++;
		// methodCost

		ArrayList<JCTree> tempContinueNodes = new ArrayList<JCTree>();
		if (init.isEmpty()) {
			ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();

			if (cond != null) {
				// fill the start/end/exit nodes
				cond.accept(this);
				finalEndNodes.addAll(currentEndNodes);

				ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
				body.accept(this);

				ArrayList<JCTree> currentExcEndNodes = new ArrayList<JCTree>(
						this.currentExitNodes);

				// for breaks;
				ArrayList<JCTree> tempBreakNodes = new ArrayList<JCTree>();
				currentExcEndNodes = resolveBreaks(tree, tempBreakNodes,
						currentExcEndNodes);

				// for continues;
				finalEndNodes.addAll(tempBreakNodes);
				currentExcEndNodes = resolveContinues(tree, tempContinueNodes,
						currentExcEndNodes);

				if (!step.isEmpty()) {
					visitStatements(tree.step);
				}

				this.currentStartNodes = currentStartNodes;
				this.currentEndNodes = finalEndNodes;
				this.currentExitNodes = currentExcEndNodes;

				addNode(tree);
			} else { /* tree.cond == null, condition is empty. */
				body.accept(this);

				ArrayList<JCTree> currentStartNodes = this.currentStartNodes;
				ArrayList<JCTree> currentExcEndNodes = new ArrayList<JCTree>(
						this.currentExitNodes);

				// for breaks;
				ArrayList<JCTree> tempBreakNodes = new ArrayList<JCTree>();
				currentExcEndNodes = resolveBreaks(tree, tempBreakNodes,
						currentExcEndNodes);

				// for continues;
				finalEndNodes.addAll(tempBreakNodes);
				currentExcEndNodes = resolveContinues(tree, tempContinueNodes,
						currentExcEndNodes);

				if (!tree.step.isEmpty()) {
					visitStatements(tree.step);
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

			if (cond != null) {
				cond.accept(this);
			}

			ArrayList<JCTree> currentEndNodes = new ArrayList<JCTree>(
					this.currentEndNodes);

			body.accept(this);
			
			ArrayList<JCTree> currentExcEndNodes = new ArrayList<JCTree>(
					this.currentExitNodes);

			// for breaks;
			ArrayList<JCTree> tempBreakNodes = new ArrayList<JCTree>();
			currentExcEndNodes = resolveBreaks(tree, tempBreakNodes,
					currentExcEndNodes);

			// for continues;
			finalEndNodes.addAll(tempBreakNodes);
			currentExcEndNodes = resolveContinues(tree, tempContinueNodes,
					currentExcEndNodes);

			finalEndNodes.addAll(currentEndNodes);

			if (!tree.step.isEmpty()) {
				visitStatements(tree.step);
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
		if (cond != null) {
			nextStartNodeTree = cond;
 
			connectStartNodesToEndNodesOf(body, cond);
		} else {
			nextStartNodeTree = body;
		}

		if (!step.isEmpty()) {
			JCTree lastStatement = visitList(step);
			if (cond != null) {
				connectStartNodesToEndNodesOf(cond, lastStatement);
			} else {
				connectStartNodesToEndNodesOf(body, lastStatement);
			}
			connectStartNodesToEndNodesOf(lastStatement, body);
		} else { connectStartNodesToEndNodesOf(nextStartNodeTree, body); }

		if (cond != null) {
			for (JCTree jct1 : tempContinueNodes) {
				jct1.successors.addAll(cond.startNodes);
				for (JCTree jct2 : cond.startNodes) {
					jct2.predecessors.add(jct1);
				}
			}
		} else {
			for (JCTree jct1 : tempContinueNodes) {
				jct1.successors.addAll(body.startNodes);
				for (JCTree jct2 : body.startNodes) {
					jct2.predecessors.add(jct1);
				}
			}
		}

		// methodCost
		this.loop--;
		// methodCost
	}

	public void visitForeachLoop(JCEnhancedForLoop tree) {
		JCExpression expr = tree.expr;
		JCStatement body = tree.body;

		// methodCost
		this.loop++;
		// methodCost

		// fill the start/end/exit nodes
		tree.id = id++;
		init(tree);
		order.add(tree);

		expr.accept(this);
		body.accept(this);

		ArrayList<JCTree> bodyExcEndNodes = currentExitNodes;

		// this.currentStartNodes = currentStartNodes;
		ArrayList<JCTree> temp_start = new ArrayList<JCTree>(1);
		temp_start.add(tree);
		this.currentStartNodes = temp_start;
		// currentEndNodes = new ArrayList<JCTree>(currentEndNodes);
		// currentEndNodes.add(tree);

		// for breaks;
		ArrayList<JCTree> tempBreakNodes = new ArrayList<JCTree>();
		ArrayList<JCTree> tempContinueNodes = new ArrayList<JCTree>();
		bodyExcEndNodes = resolveBreaks(tree, tempBreakNodes, bodyExcEndNodes);

		// for continues;
		bodyExcEndNodes = resolveContinues(tree, tempContinueNodes,
				bodyExcEndNodes);

		currentExitNodes = bodyExcEndNodes;

		// addNode(tree);
		tree.startNodes = currentStartNodes;
		tree.endNodes = currentEndNodes;
		tree.exitNodes = currentExitNodes;

		for (JCTree jct : tempBreakNodes) {
			jct.successors.add(tree);
			tree.predecessors.add(jct);
		}

		// connect the nodes
		connectToStartNodesOf(tree, expr);
		// connectToEndNodesOf(expr, tree);

		connectStartNodesToEndNodesOf(body, expr);

		connectStartNodesToEndNodesOf(body, body);

		for (JCTree jct1 : tempContinueNodes) {
			jct1.successors.addAll(body.startNodes);
			for (JCTree jct2 : body.startNodes) {
				jct2.predecessors.add(jct1);
			}
		}

		// methodCost
		this.loop--;
		// methodCost
	}

	/* used by visitSwitch and visitCase only, which visit the single node then
	 * the subsequent list. */
	public void switchAndCase(JCTree single, List<? extends JCTree> list) {
		if (list.head != null) {
			connectStartNodesToEndNodesOf(list.head, single);
			JCTree prev = list.head;
			for (JCTree tree : list.tail) {
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

		JCTree head = cases.head;
		if (head != null) {
			ArrayList<JCTree> finalEndNodes = new ArrayList<JCTree>();
			ArrayList<JCTree> selectorEndNodes = currentEndNodes;
			ArrayList<JCTree> finalExcEndNodes = new ArrayList<JCTree>();

			ArrayList<JCTree> previousEndNodes = null;
			// for breaks;
			ArrayList<JCTree> tempBreakNodes = new ArrayList<JCTree>();

			boolean hasDefault = false;

			for (List<JCCase> l = tree.cases; l.nonEmpty(); l = l.tail) {
				JCCase c = l.head;
				if (c.pat == null) {
					hasDefault = true;
				}

				c.accept(this);

				if (previousEndNodes != null) {
					for (JCTree pre : previousEndNodes) {
						connectStartNodesToEndNodesOf(c, pre);
					}
				}

				connectStartNodesToEndNodesOf(c, selector);

				ArrayList<JCTree> currentExcEndNodes = currentExitNodes;

                // for breaks;
    			currentExcEndNodes =
    				resolveBreaks(tree, tempBreakNodes, currentExcEndNodes);
    			
    			previousEndNodes = currentEndNodes;
    			finalExcEndNodes.addAll(currentExcEndNodes);
			}

			if (!hasDefault) {
				finalEndNodes.addAll(selectorEndNodes);
			}

			finalEndNodes.addAll(tempBreakNodes);

			this.currentEndNodes = finalEndNodes;
			this.currentExitNodes = finalExcEndNodes;
		}

		currentStartNodes = new ArrayList<JCTree>(1);
		currentStartNodes.add(tree);

		addNode(tree);

		connectToStartNodesOf(tree, selector);
	}

	public void visitCase(JCCase tree) {
		JCExpression pat = tree.pat;
		List<JCStatement> stats = tree.stats;

		// the default case does not have a pat
		if (pat != null) {
			// fill the start/end/exit nodes
			pat.accept(this);
			ArrayList<JCTree> currentStartNodes = this.currentStartNodes;

			visitStatements(stats);

			this.currentStartNodes = currentStartNodes;

			addNode(tree);

			// connect the nodes
			switchAndCase(pat, stats);
		} else {
			visitStatements(stats);

			addNode(tree);
		}
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
		// singleton(tree);
		currentEndNodes = emptyList;
		currentExitNodes = new ArrayList<JCTree>(1);
		currentExitNodes.add(tree);

		currentStartNodes = new ArrayList<JCTree>(1);
		currentStartNodes.add(tree);
		addNode(tree);

		// methodCost
		this.methodCost += Costs.goto_;
		// methodCost
		// connect the nodes
	}

	public void visitContinue(JCContinue tree) {
		// fill the start/end/exit nodes
		singleton(tree);
		currentEndNodes = emptyList;
		currentExitNodes = new ArrayList<JCTree>(1);
		currentExitNodes.add(tree);

		currentStartNodes = new ArrayList<JCTree>(1);
		currentStartNodes.add(tree);
		addNode(tree);

		// methodCost
		this.methodCost += Costs.goto_;
		// methodCost
	}

	public void visitReturn(JCReturn tree) {
		// fill the start/end/exit nodes
		JCExpression expr = tree.expr;
		// methodCost
		// TODO: differenciate b.w various possible return opcodes.
		this.methodCost += Costs.ireturn;
		// methodCost
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
			connectToEndNodesOf(expr, tree);
		}
	}

	public void visitThrow(JCThrow tree) {
		// fill the start/end/exit nodes
		JCExpression expr = tree.expr;
		// methodCost
		this.methodCost += Costs.athrow;
		// methodCost
		expr.accept(this);
		currentEndNodes = emptyList;
		currentExitNodes = new ArrayList<JCTree>(1);
		currentExitNodes.add(tree);
		addNode(tree);

		// connect the nodes
		// if (expr != null) {
		connectToEndNodesOf(expr, tree);
		// }
	}

	public void visitApply(JCMethodInvocation tree) {
		JCExpression meth = tree.meth;
		List<JCExpression> args = tree.args;

		// TODO:need to identify type of invocation (static, virtual, special,
		// interface or dynamic).
		methodCost += Costs.invokespecial;
		// methodCost
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
			connectStartNodesToEndNodesOf(args.head, meth);
			connectToEndNodesOf(lastArg, tree);
		} else {
			connectToEndNodesOf(meth, tree);
		}
	}

	public void visitNewClass(JCNewClass tree) {
		List<JCExpression> args = tree.args;
		// methodCost
		// emits new_ and dup
		methodCost += Costs.new_;
		methodCost += Costs.dup;
		// methodCost
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
			connectToEndNodesOf(lastArg, tree);
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

				if (dims.isEmpty()) {
					finalStartNodes = currentStartNodes;
				}
			}
		}

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		currentExitNodes = finalExcEndNodes;

		if (finalStartNodes == null) {
			currentStartNodes = new ArrayList<JCTree>(1);
			currentStartNodes.add(tree);
		} else {
			currentStartNodes = finalStartNodes;
		}

		addNode(tree);

		// connect the nodes
		if (!dims.isEmpty()) {
			JCTree lastDimension = visitList(dims);

			if (elems != null) {
				if (!elems.isEmpty()) {
					JCTree lastElement = visitList(elems);

					connectStartNodesToEndNodesOf(elems.head, lastDimension);
					connectToEndNodesOf(lastElement, tree);
				} else {
					connectToEndNodesOf(lastDimension, tree);
				}
			} else {
				connectToEndNodesOf(lastDimension, tree);
			}
		} else {
			if (elems != null) {
				if (!elems.isEmpty()) {
					JCTree lastElement = visitList(elems);

					connectToEndNodesOf(lastElement, tree);
				}
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
		// methodCost
		// TODO: basically have to load the value and store it
		// but, at this point cannot get the type of the value
		methodCost += Costs.iload;
		methodCost += Costs.istore;
		// methodCost
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
		connectToEndNodesOf(rhs, tree);
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
		connectToEndNodesOf(rhs, tree);
	}

	public void visitUnary(JCUnary tree) {
		// fill the start/end/exit nodes
		tree.arg.accept(this);

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		addNode(tree);

		// connect the nodes
		connectToEndNodesOf(tree.arg, tree);
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
		connectToEndNodesOf(rhs, tree);
	}

	public void visitTypeCast(JCTypeCast tree) {
		JCExpression expr = tree.expr;

		// fill the start/end/exit nodes
		expr.accept(this);

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		addNode(tree);

		// connect the nodes
		connectToEndNodesOf(expr, tree);
	}

	public void visitTypeTest(JCInstanceOf tree) {
		JCExpression expr = tree.expr;

		// fill the start/end/exit nodes
		expr.accept(this);

		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		addNode(tree);

		// connect the nodes
		connectToEndNodesOf(expr, tree);
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
		connectToEndNodesOf(index, tree);
	}

	public void visitSelect(JCFieldAccess tree) {
		JCExpression selected = tree.selected;
		// methodCost
		Symbol s = tree.sym;
		if ((s != null) && s.isStatic())
			methodCost += Costs.getstatic;
		else
			methodCost += Costs.getfield;
		// methodCost

		// fill the start/end/exit nodes
		selected.accept(this);
		currentEndNodes = new ArrayList<JCTree>(1);
		currentEndNodes.add(tree);

		currentExitNodes = emptyList;

		addNode(tree);

		// connect the nodes
		connectToEndNodesOf(selected, tree);
	}

	private JCTree visitList(List<? extends JCTree> trees) {
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
			if (endNode.successors != null) {
				endNode.successors.add(end);
			}
			if (end.predecessors != null) {
				end.predecessors.add(endNode);
			}
		}
	}

	private static void connectStartNodesToEndNodesOf(JCTree start,
			JCTree end) {
		for (JCTree endNode : end.endNodes) {
			for (JCTree startNode : start.startNodes) {
				endNode.successors.add(startNode);
				startNode.predecessors.add(endNode);
			}
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
			this.currentExitNodes = finalExcEndNodes;
		}
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
		order.add(tree);
	}

	private static ArrayList<JCTree> resolveBreaks(JCTree target,
			ArrayList<JCTree> endNodes, ArrayList<JCTree> nodes) {		
		ArrayList<JCTree> remaining = new ArrayList<JCTree>();
		for (JCTree tree : nodes) {
			boolean found = false;
			if (tree instanceof JCBreak) {
				JCBreak jcb = (JCBreak)tree;
				if (jcb.target == target) {
					endNodes.add(tree);
					found = true;
				}
			}
			if (!found) {
				remaining.add(tree);
			}
		}
		return remaining;
	}

	private static ArrayList<JCTree> resolveContinues(JCTree target,
			ArrayList<JCTree> targets, ArrayList<JCTree> nodes) {
		ArrayList<JCTree> results = new ArrayList<JCTree>();
		for (JCTree tree : nodes) {
			boolean found = false;
			if (tree instanceof JCContinue) {
				JCContinue jcc = (JCContinue)tree;
				if (jcc.target == target) {
					jcc.successors.add(target);
					targets.add(jcc);
				}
			}
			if (!found) {
				results.add(tree);
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
		}
	}
}