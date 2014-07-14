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
 * Contributor(s): Yuheng Long
 */

package org.paninij.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;

public class LeakDetection {
	private List<JCTree> defs;
	private HashMap<Symbol, HashSet<Symbol>> reversed =
		new HashMap<Symbol, HashSet<Symbol>>();
	private HashMap<Symbol, HashSet<Symbol>> result =
		new HashMap<Symbol, HashSet<Symbol>>();

	private boolean analyzingphase = true;
	private Log log;
	private JCCapsuleDecl capsule;
	private JCMethodDecl curr;
	private InnerClassCapsuleAliasDetector icca;

	private HashMap<Symbol, HashSet<JCTree>> all_nodes =
		new HashMap<Symbol, HashSet<JCTree>>();

	// the intermediate result from the intra procedural analysis.
	private HashMap<JCTree, TreeWrapper> backwardMap =
		new HashMap<JCTree, TreeWrapper>();

	private HashMap<JCTree, TreeWrapper> forwardMap =
		new HashMap<JCTree, TreeWrapper>();

	private HashSet<String> warnings = new HashSet<String>();

	// state that is assigned to new object before any use.
	private HashSet<Symbol> nonLeakStates;

	public void inter(JCCapsuleDecl capsule, Log log) {
		this.log = log;
		this.capsule = capsule;
		ClassSymbol cs = capsule.sym;

		icca = new InnerClassCapsuleAliasDetector(log);

		defs = capsule.defs;
		TreeSet<MethodWrapper> queue = new TreeSet<MethodWrapper>();
		HashMap<JCMethodDecl, MethodWrapper> map =
			new HashMap<JCMethodDecl, MethodWrapper>();

		boolean noReturnLeak = true;
		for (JCTree jct : defs) {
			if (jct instanceof JCMethodDecl) {
				JCMethodDecl meth = (JCMethodDecl)jct;

				if (AnalysisUtil.shouldLeakAnalyze(capsule, meth) &&
						meth.body != null) {
					if (!noReturnLeak(meth, cs)) {
						noReturnLeak = false;
					}

					meth.body.accept(icca);
					MethodWrapper temp = new MethodWrapper(meth);
					map.put(meth, temp);
					queue.add(temp);
				}
			}
		}

		// if a capsule does not have reference to other capsule and if it only
		// returns primitives or new objects, it will never leak.
		if (AnalysisUtil.leafCapsule(capsule) && noReturnLeak) {
			return;
		}

		nonLeakStates = new HashSet<Symbol>();
		if (AnalysisUtil.leafCapsule(capsule)) {
			for (Symbol s : AnalysisUtil.allCapsuleStates(capsule)) {
				FreshStateDetection fsd = new FreshStateDetection(s);

				for (JCMethodDecl jcmd : map.keySet()) {
					if (fsd.nonLeak) {
						fsd.scan(jcmd);
					}
				}
				if (fsd.nonLeak) {
					nonLeakStates.add(s);
				}
			}
		}

		while (!queue.isEmpty()) {
			MethodWrapper w = queue.first();
			queue.remove(w);
			JCMethodDecl curr = w.meth;

			HashSet<Symbol> previous = result.get(curr.sym);
			HashSet<Symbol> newResult = backwardsintra(capsule, curr);
			forwardintra(capsule, curr);

			if (previous == null || !previous.equals(newResult)) {
				result.put(curr.sym, newResult);
				HashSet<Symbol> callers = reversed.get(curr.sym);
				if (callers != null) {
					for (Symbol caller : callers) {
						MethodSymbol ms = (MethodSymbol)caller;
						queue.add(map.get(ms.tree));
					}
				}
			}
		}

		// output warnings
		analyzingphase = false;
		for (JCTree jct : defs) {
			if (jct instanceof JCMethodDecl) {
				JCMethodDecl jcmd = (JCMethodDecl) jct;
				if (AnalysisUtil.shouldLeakAnalyze(capsule, jcmd)) {
					if (jcmd.body != null) {
						backwardsintra(capsule, jcmd);

						forwardintra(capsule, jcmd);
					}
				}
			}
		}
	}

	private static boolean noLeakNew(JCNewClass jcr) {
		for (JCExpression arg : jcr.args) {
			arg = AnalysisUtil.getEssentialExpr(arg);
			if (arg.type.isPrimitive()) {
				continue;
			}
			if (arg instanceof JCNewClass) {
				JCNewClass jcnc = (JCNewClass)arg;
				if (noLeakNew(jcnc)) {
					continue;
				}
			}
			return false;
		}
		return false;
	}
	// whether a method can leak object by return
	private static boolean noReturnLeak(JCMethodDecl jcmd, ClassSymbol cs) {
		String tree_name = jcmd.sym.toString();

		if (!AnalysisUtil.activeThread(cs, tree_name)) {
			String capsule_name = cs.toString();
			if ((capsule_name.substring(capsule_name.indexOf("$")
					+ 1).compareTo("thread") == 0) &&
					(tree_name.indexOf("$Original") == -1)) {
				return true;
			}
		}

		Type type = jcmd.restype.type;
		if (type.getKind() == TypeKind.VOID) {
			return true;
		}

		JCTree body = jcmd.body;
		ArrayList<JCTree> finalNodes = new ArrayList<JCTree>(body.exitNodes);
		finalNodes.addAll(body.endNodes);
		for (JCTree endNode : finalNodes) {
			if (endNode instanceof JCReturn) {
				JCReturn jcr = (JCReturn)endNode;
				JCExpression arg = AnalysisUtil.getEssentialExpr(jcr.expr);
				if (arg.type.isPrimitive()) {
					continue;
				} else if (arg instanceof JCNewClass) {
					JCNewClass jcnc = (JCNewClass)arg;
					if (!noLeakNew(jcnc)) { return false; }
					continue;
				}
				return false;
			}
		}
		return true;
	}

	private void forwardintra(JCCapsuleDecl capsule, JCMethodDecl meth) {
		curr = meth;
		defs = capsule.defs;

		JCBlock body = meth.body;
		assert body != null;

		TreeSet<TreeWrapper> queue = new TreeSet<TreeWrapper>();

		for (JCTree tree : body.startNodes) {
			TreeWrapper temp = new TreeWrapper(tree);
			if (analyzingphase) {
				forwardMap.put(tree, temp);
			}
			queue.add(temp);
		}

		HashSet<JCTree> nodes = all_nodes.get(meth.sym);
		if (analyzingphase) {
			if (nodes != null) {
				for (JCTree node : nodes) {
					TreeWrapper temp = new TreeWrapper(node);		
					forwardMap.put(node, temp);

					queue.add(temp);
				}
			}
		}

		HashSet<JCTree> processed = new HashSet<JCTree>();

		while (!queue.isEmpty()) {
			TreeWrapper w = queue.first();
			queue.remove(w);
			JCTree curr = w.tree;

			if (!analyzingphase) {
				if (processed.contains(curr)) { continue; }
				processed.add(curr);
			}

			HashSet<Symbol> preLeak = new HashSet<Symbol>();
			for (JCTree succ : curr.predecessors) {
				TreeWrapper temp = forwardMap.get(succ);
				if (temp == null) {
					temp = new TreeWrapper(succ);
					forwardMap.put(succ, temp);
				}

				preLeak.addAll(temp.leakVar);
			}

			HashSet<Symbol> previous = new HashSet<Symbol>(w.leakVar);
			forwardVisitTree(curr, preLeak, meth);

			if (analyzingphase) {
				if (!preLeak.equals(previous) || w.first) {
					w.leakVar.addAll(preLeak);
					w.first = false;
					for (JCTree predecessor : curr.successors) {
						TreeWrapper temp = forwardMap.get(predecessor);
						if (temp == null) {
							temp = new TreeWrapper(predecessor);
							forwardMap.put(predecessor, temp);
						}
						queue.add(temp);
					}
				}
			} else {
				for (JCTree predecessor : curr.successors) {
					TreeWrapper temp = forwardMap.get(predecessor);
					queue.add(temp);
				}
			}
		}
	}

	public HashSet<Symbol> backwardsintra(JCCapsuleDecl capsule,
			JCMethodDecl meth) {
		curr = meth;
		defs = capsule.defs;

		JCBlock body = meth.body;
		assert body != null;

		TreeSet<TreeWrapper> queue = new TreeSet<TreeWrapper>();

		for (JCTree tree : body.endNodes) {
			TreeWrapper temp = new TreeWrapper(tree);
			if (analyzingphase) {
				backwardMap.put(tree, temp);
			}
			queue.add(temp);
		}
		for (JCTree tree : body.exitNodes) {
			TreeWrapper temp = new TreeWrapper(tree);
			if (analyzingphase) {
				backwardMap.put(tree, temp);
			}
			queue.add(temp);
		}

		HashSet<JCTree> nodes = all_nodes.get(meth.sym);
		if (analyzingphase) {
			if (nodes != null) {
				for (JCTree node : nodes) {
					TreeWrapper temp = new TreeWrapper(node);		
					backwardMap.put(node, temp);

					queue.add(temp);
				}
			}
		}

		HashSet<JCTree> processed = new HashSet<JCTree>();

		while (!queue.isEmpty()) {
			TreeWrapper w = queue.first();
			queue.remove(w);
			JCTree curr = w.tree;

			if (!analyzingphase && processed.contains(curr)) {
				continue;
			}

			processed.add(curr);

			HashSet<Symbol> preLeak = new HashSet<Symbol>();
			for (JCTree succ : curr.successors) {
				TreeWrapper temp = backwardMap.get(succ);
				if (temp == null) {
					temp = new TreeWrapper(succ);
					backwardMap.put(succ, temp);
				}

				preLeak.addAll(temp.leakVar);
			}
			HashSet<Symbol> previous = new HashSet<Symbol>(w.leakVar);

			visitTree(curr, preLeak, meth, false);

			if (analyzingphase) {
				if (!preLeak.equals(previous) || w.first) {
					w.leakVar.addAll(preLeak);
					w.first = false;
					for (JCTree predecessor : curr.predecessors) {
						TreeWrapper temp = backwardMap.get(predecessor);
						if (temp == null) {
							temp = new TreeWrapper(predecessor);
							backwardMap.put(predecessor, temp);
						}
						queue.add(temp);
					}
				}
			} else {
				for (JCTree predecessor : curr.predecessors) {
					TreeWrapper temp = backwardMap.get(predecessor);
					queue.add(temp);
				}
			}
		}

		if (nodes == null) {
			nodes = processed;
			all_nodes.put(meth.sym, processed);
		}

		HashSet<Symbol> intraResult = new HashSet<Symbol>();
		for (JCTree jct : backwardMap.keySet()) {
			if (nodes.contains(jct)) {
				TreeWrapper jw = backwardMap.get(jct);
				intraResult.addAll(jw.leakVar);
			}
		}

		return intraResult;
	}

	private void forwardVisitTree(JCTree curr, HashSet<Symbol> preLeak,
			JCMethodDecl meth) {
		if (curr instanceof JCIdent) { // T var = init;
			JCIdent jci = (JCIdent) curr;
			Symbol sym = jci.sym;
			if (preLeak.contains(sym)) {
				if (sym.getKind() != ElementKind.FIELD) {
					warning((JCIdent)curr, preLeak, true, true);
				}
			}
		} else if (curr instanceof JCMethodInvocation) {
			JCMethodInvocation jcmi = (JCMethodInvocation)curr;
			JCExpression currMeth = jcmi.meth;

			boolean innerCall = false;

			// if the method is not intra capsule method call
			if (currMeth instanceof JCIdent) {
				JCIdent jci = (JCIdent) currMeth;
				innerCall = true;
				AnalysisUtil.addCallEdge(meth.sym, jci.sym, reversed);

				HashSet<Symbol> leaks = result.get(jci.sym);
				if (leaks != null) {
					for (Symbol s : leaks) {
						if (s != null) {
							ElementKind symKind = s.getKind();
							if (symKind == ElementKind.FIELD) {
								preLeak.add(s);
							} else if (symKind == ElementKind.PARAMETER) {
								MethodSymbol ms = (MethodSymbol)jci.sym;
								int i = 0;
								for (Symbol para : ms.params) {
									if (para == s) {
										warningCandidates(jcmi.args.get(i),
												preLeak, true);
									}
									i++;
								}
							}
						}
					}
				}
			} else if (currMeth instanceof JCFieldAccess) {
				JCFieldAccess jcfa = (JCFieldAccess)currMeth;
				JCExpression receiver =
					AnalysisUtil.getEssentialExpr(jcfa.selected);

				if (receiver instanceof JCIdent) {
					JCIdent jci = (JCIdent)receiver;
					if (AnalysisUtil.isInnerField(defs, jci.sym)) {
						innerCall = true;
					}
				} else if (receiver instanceof JCFieldAccess) {
					JCFieldAccess field = (JCFieldAccess)receiver;
					if (AnalysisUtil.isInnerField(defs, field.sym)) {
						innerCall = true;
					}
				}
			}

			// If this is inter procedural capsule call, all the parameters
			// will be leaked.
			if (!innerCall) { warningList(jcmi.args, preLeak, true); }
		} else if (curr instanceof JCVariableDecl) {
			JCVariableDecl jcvd = (JCVariableDecl)curr;
			JCExpression init = jcvd.init;
			if (init != null && AnalysisUtil.isNewExpression(init)) {
				preLeak.remove(jcvd.sym);
			}
		} else if (curr instanceof JCAssign) {
			JCAssign jca = (JCAssign)curr;
			JCExpression lhs = AnalysisUtil.getEssentialExpr(jca.lhs);
			JCExpression rhs = AnalysisUtil.getEssentialExpr(jca.rhs);
			
			if (AnalysisUtil.isNewExpression(rhs) && lhs instanceof JCIdent) {
				JCIdent jci = (JCIdent)lhs;
				preLeak.remove(jci.sym);
			}
		}
	}

	private void visitTree(JCTree curr, HashSet<Symbol> preLeak,
			JCMethodDecl meth, boolean warnLeakingVar) {
		if (curr instanceof JCVariableDecl) { // T var = init;
			JCVariableDecl jcvd = (JCVariableDecl)curr;
			if (preLeak.contains(jcvd.sym)) {
				warningCandidates(jcvd.init, preLeak, warnLeakingVar);
			}
		} else if (curr instanceof JCReturn) { // return exp;
			warningCandidates(((JCReturn)curr).expr, preLeak, warnLeakingVar);
		} else if (curr instanceof JCMethodInvocation) {
			JCMethodInvocation jcmi = (JCMethodInvocation)curr;
			JCExpression currMeth = jcmi.meth;

			boolean innerCall = false;

			// if the method is not intra capsule method call
			if (currMeth instanceof JCIdent) {
				JCIdent jci = (JCIdent) currMeth;
				innerCall = true;
				AnalysisUtil.addCallEdge(meth.sym, jci.sym, reversed);
				HashSet<Symbol> leaks = result.get(jci.sym);

				if (leaks != null) {
					for (Symbol s : leaks) {
						if (s != null) {
							ElementKind symKind = s.getKind();
							if (symKind == ElementKind.FIELD) {
								preLeak.add(s);
							} else if (symKind == ElementKind.PARAMETER) {
								MethodSymbol ms = (MethodSymbol)jci.sym;
								int i = 0;
								for (Symbol para : ms.params) {
									if (para == s) {
										warningCandidates(jcmi.args.get(i),
												preLeak, warnLeakingVar);
									}
									i++;
								}
							}
						}
					}
				}
			} else if (currMeth instanceof JCFieldAccess) {
				JCFieldAccess jcfa = (JCFieldAccess)currMeth;
				JCExpression receiver =
					AnalysisUtil.getEssentialExpr(jcfa.selected);

				if (receiver instanceof JCIdent) {
					JCIdent jci = (JCIdent)receiver;
					if (AnalysisUtil.isInnerField(defs, jci.sym)) {
						innerCall = true;
					}
				} else if (receiver instanceof JCFieldAccess) {
					JCFieldAccess field = (JCFieldAccess)receiver;
					if (AnalysisUtil.isInnerField(defs, field.sym)) {
						innerCall = true;
					}
				}
			}

			// If this is inter procedural capsule call, all the parameters
			// will be leaked.
			if (!innerCall) {
				warningList(jcmi.args, preLeak, warnLeakingVar);
			}
		} else if (curr instanceof JCAssign) {
			JCAssign jca = (JCAssign)curr;
			JCExpression lhs = AnalysisUtil.getEssentialExpr(jca.lhs);
			JCExpression rhs = AnalysisUtil.getEssentialExpr(jca.rhs);
			if (lhs instanceof JCIdent) {
				JCIdent jci = (JCIdent)lhs;
				if (preLeak.contains(jci.sym)) {
					warningCandidates(rhs, preLeak, warnLeakingVar);
				}
			} else if (lhs instanceof JCFieldAccess) {
				JCFieldAccess jcf = (JCFieldAccess)lhs;

				if (AnalysisUtil.isVarThis(jcf.selected)) {
					if (preLeak.contains(jcf.sym)) {
						warningCandidates(rhs, preLeak, warnLeakingVar);
					}
				} else { warningCandidates(rhs, preLeak, warnLeakingVar); }
			} else {
				warningCandidates(rhs, preLeak, warnLeakingVar);
			}

			if (rhs instanceof JCIdent) {
				JCIdent jci = (JCIdent)rhs;
				if (preLeak.contains(jci.sym)) {
					warningCandidates(lhs, preLeak, warnLeakingVar);
				}
			}
		} else if (curr instanceof JCNewClass) {
			warningList(((JCNewClass)curr).args, preLeak, warnLeakingVar);
		} else if (curr instanceof JCNewArray) {
			warningList(((JCNewArray)curr).elems, preLeak, warnLeakingVar);
		}
	}

	private static class MethodWrapper implements Comparable<MethodWrapper> {
		public final JCMethodDecl meth;

		public MethodWrapper(JCMethodDecl meth) { this.meth = meth; }

		public int compareTo(MethodWrapper o) {
			return o.meth.hashCode() - meth.hashCode();
		}
	}

	private static class TreeWrapper implements Comparable<TreeWrapper> {
		public boolean first = true;
		public HashSet<Symbol> leakVar = new HashSet<Symbol>();
		public final JCTree tree;

		public TreeWrapper(JCTree tree) { this.tree = tree; }

		public int compareTo(TreeWrapper o) {
			return o.tree.hashCode() - tree.hashCode();
		}
	}

	private void warningList(List<? extends JCTree> listsOfTrees,
			HashSet<Symbol> output, boolean warnLeakingVar) {
		if (listsOfTrees != null) {
			for (JCTree jct : listsOfTrees) {
				warningCandidates(jct, output, warnLeakingVar);
			}
		}
	}

	private void warnJCFieldAccess(JCTree tree, HashSet<Symbol> output,
			boolean mustBePrimitive) {
		JCFieldAccess jcfa = (JCFieldAccess)tree;
		if (!mustBePrimitive || !jcfa.type.isPrimitive()) {
			Symbol field_sym = jcfa.sym;
			output.add(field_sym);
			if (!analyzingphase) {
				JCExpression selected = jcfa.selected;
				if (AnalysisUtil.isVarThis(selected) &&
						AnalysisUtil.isInnerField(defs, field_sym)) {
					Symbol capSym = capsule.sym;
					Symbol meth = curr.sym;
					if (!AnalysisUtil.immute_type(field_sym.type)) {
						String meth_string = meth.toString();

						addWarnings(meth_string, tree, field_sym, capSym);
					}
				}
			}
		}
	}

	private void addWarnings(String meth_string, JCTree tree, Symbol sym,
			Symbol capSym) {
		String warningStr = tree.getPreferredPosition() +
			"confinement.violation" + sym +
			AnalysisUtil.rmDollar(capSym.toString()) +
			AnalysisUtil.rmDollar(meth_string);

		if (!warnings.contains(warningStr) &&
				(nonLeakStates == null || !nonLeakStates.contains(sym))) {
			warnings.add(warningStr);

			log.useSource (sym.outermostClass().sourcefile);
			log.warning(tree.pos(), "confinement.violation", sym,
					AnalysisUtil.rmDollar(capSym.toString()),
					AnalysisUtil.rmDollar(meth_string));
		}
	}

	private void warningCandidates(JCTree tree, HashSet<Symbol> output,
			boolean warnLeakingVar) {
		if (tree != null) {
			if (tree instanceof JCExpression) {
				JCExpression jce = (JCExpression)tree;
				tree = AnalysisUtil.getEssentialExpr(jce);
			}

			if (tree instanceof JCIdent) {
				warning((JCIdent) tree, output, true, warnLeakingVar);
			} else if (tree instanceof JCFieldAccess) {
				warnJCFieldAccess(tree, output, true);
			} else if (tree instanceof JCArrayAccess) {
				if (!tree.type.isPrimitive()) {
					JCArrayAccess jcaa = (JCArrayAccess)tree;
					JCExpression indexed =
						AnalysisUtil.getEssentialExpr(jcaa.indexed);
					warningCandidates(indexed, output, warnLeakingVar);
				}
			} else if (tree instanceof JCNewClass) {
				JCNewClass new_class = (JCNewClass) tree;
				new LeakExpressions(output, warnLeakingVar).scan(new_class.def);

				warningList(new_class.args, output, warnLeakingVar);
			}
		}
	}

	private void warning(JCIdent tree, HashSet<Symbol> output,
			boolean mustBePrimitive, boolean warnLeakingVar) {
		Symbol sym = tree.sym;
		if (sym != null) {
			if (!mustBePrimitive || !sym.type.isPrimitive()) {
				if (!analyzingphase) {
					if ((warnLeakingVar && output.contains(sym)) ||
							AnalysisUtil.isInnerField(defs, sym)) {
						if (!AnalysisUtil.immute_type(sym.type)) {
							Symbol curr_sym = curr.sym;

							addWarnings(curr_sym.toString(), tree, sym, capsule.sym);
						}
					}
				}
				output.add(sym);
			}
		}
	}

	public class LeakExpressions extends TreeScanner {
		public LeakExpressions(HashSet<Symbol> output, boolean warnLeakingVar) {
			this.output = output;
			this.warnLeakingVar = warnLeakingVar;
		}

		public HashSet<Symbol> output;
		public boolean warnLeakingVar;

		public void visitIdent(JCIdent tree) {
			warning(tree, output, false, warnLeakingVar); }

		public void visitSelect(JCFieldAccess tree) {
			warnJCFieldAccess(tree, output, false);
	    }

		public void visitNewClass(JCNewClass tree) {
			new LeakExpressions(output, warnLeakingVar).scan(tree.def);
	    } 
	}
}
