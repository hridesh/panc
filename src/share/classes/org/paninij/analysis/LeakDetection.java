package org.paninij.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import javax.lang.model.element.ElementKind;

import com.sun.tools.javac.code.Symbol;
// import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
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

	// the intermediate result from the intra procedural analysis.
	private HashMap<JCTree, TreeWrapper> intraMap =
		new HashMap<JCTree, TreeWrapper>();

public static boolean DEBUG = false;
	public void inter(JCCapsuleDecl capsule, Log log) {
		this.log = log;
		this.capsule = capsule;

		icca = new InnerClassCapsuleAliasDetector(log);

		defs = capsule.defs;
		TreeSet<MethodWrapper> queue = new TreeSet<MethodWrapper>();
		HashMap<JCMethodDecl, MethodWrapper> map =
			new HashMap<JCMethodDecl, MethodWrapper>();

		for (JCTree jct : defs) {
			if (jct instanceof JCMethodDecl) {
				if (((JCMethodDecl) jct).body != null) {
					JCMethodDecl meth = (JCMethodDecl)jct;

					if (AnalysisUtil.shouldAnalyze(capsule, meth)) {
						if (meth.body != null) {
							meth.body.accept(icca);

							MethodWrapper temp = new MethodWrapper(meth);
							map.put(meth, temp);
							queue.add(temp);
						}
					}
				}
			}
		}

		while (!queue.isEmpty()) {
			MethodWrapper w = queue.first();
			queue.remove(w);
			JCMethodDecl curr = w.meth;

			HashSet<Symbol> previous = result.get(curr.sym);
			HashSet<Symbol> newResult = intra(capsule, curr);

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
				if (AnalysisUtil.shouldAnalyze(capsule, jcmd)) {
					if (jcmd.body != null) {
						intra(capsule, jcmd);
					}
				}
			}
		}
	}

	public HashSet<Symbol> intra(JCCapsuleDecl capsule, JCMethodDecl meth) {
		curr = meth;
		defs = capsule.defs;

		JCBlock body = meth.body;
		assert body != null;

		TreeSet<TreeWrapper> queue = new TreeSet<TreeWrapper>();

		for (JCTree tree : body.endNodes) {
			TreeWrapper temp = new TreeWrapper(tree);
			if (analyzingphase) {
				intraMap.put(tree, temp);
			}
			queue.add(temp);
		}
		for (JCTree tree : body.exitNodes) {
			TreeWrapper temp = new TreeWrapper(tree);
			if (analyzingphase) {
				intraMap.put(tree, temp);
			}
			queue.add(temp);
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
			for (JCTree succ : curr.successors) {
				TreeWrapper temp = intraMap.get(succ);
				if (temp == null) {
					temp = new TreeWrapper(succ);
					intraMap.put(succ, temp);
				}

				preLeak.addAll(temp.leakVar);
			}
			HashSet<Symbol> previous = new HashSet<Symbol>(w.leakVar);

			if (curr instanceof JCVariableDecl) { // T var = init;
				JCVariableDecl jcvd = (JCVariableDecl)curr;
				if (preLeak.contains(jcvd.sym)) {
					warningCandidates(jcvd.init, preLeak);
				}
			} else if (curr instanceof JCReturn) { // return exp;
				warningCandidates(((JCReturn)curr).expr, preLeak);
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
													preLeak);
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
					warningList(jcmi.args, preLeak); }
			} else if (curr instanceof JCAssign) {
				JCAssign jca = (JCAssign)curr;
				JCExpression lhs = AnalysisUtil.getEssentialExpr(jca.lhs);
				if (lhs instanceof JCIdent) {
					JCIdent jci = (JCIdent)lhs;
					if (preLeak.contains(jci.sym)) {
						warningCandidates(jca.rhs, preLeak);
					}
				} else if (lhs instanceof JCFieldAccess) {
					JCFieldAccess jcf = (JCFieldAccess)lhs;

					if (AnalysisUtil.isVarThis(jcf.selected)) {
						if (preLeak.contains(jcf.sym)) {
							warningCandidates(jca.rhs, preLeak);
						}
					} else { warningCandidates(jca.rhs, preLeak); }
				} else {
					warningCandidates(jca.rhs, preLeak);
				}
			} else if (curr instanceof JCNewClass) {
				warningList(((JCNewClass)curr).args, preLeak);
			} else if (curr instanceof JCNewArray) {
				warningList(((JCNewArray)curr).elems, preLeak);
			}

			if (analyzingphase) {
				if (!preLeak.equals(previous) || w.first) {
					w.leakVar.addAll(preLeak);
					w.first = false;
					for (JCTree predecessor : curr.predecessors) {
						TreeWrapper temp = intraMap.get(predecessor);
						if (temp == null) {
							temp = new TreeWrapper(predecessor);
							intraMap.put(predecessor, temp);
						}
						queue.add(temp);
					}
				}
			} else {
				for (JCTree predecessor : curr.predecessors) {
					TreeWrapper temp = intraMap.get(predecessor);
					queue.add(temp);
				}
			}
		}

		HashSet<Symbol> intraResult = new HashSet<Symbol>();
		for (JCTree jct : intraMap.keySet()) {
			TreeWrapper jw = intraMap.get(jct);
			intraResult.addAll(jw.leakVar);
		}

		return intraResult;
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
			HashSet<Symbol> output) {
		if (listsOfTrees != null) {
			for (JCTree jct : listsOfTrees) {
				warningCandidates(jct, output);
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
						log.useSource (field_sym.outermostClass().sourcefile);
						log.warning(tree.pos(), "confinement.violation",
								field_sym,
								AnalysisUtil.rmDollar(capSym.toString()),
								AnalysisUtil.rmDollar(meth_string));
					}
				}
			}
		}
	}

	private void warningCandidates(JCTree tree, HashSet<Symbol> output) {
		if (tree != null) {
			if (tree instanceof JCExpression) {
				JCExpression jce = (JCExpression)tree;
				tree = AnalysisUtil.getEssentialExpr(jce);
			}

			if (tree instanceof JCIdent) {
				warning((JCIdent) tree, output, true);
			} else if (tree instanceof JCFieldAccess) {
				warnJCFieldAccess(tree, output, true);
			} else if (tree instanceof JCNewClass) {
				new LeakExpressions(output).scan(((JCNewClass) tree).def);
			}
		}
	}

	private void warning(JCIdent tree, HashSet<Symbol> output,
			boolean mustBePrimitive) {
		Symbol sym = tree.sym;
		if (sym != null) {
			if (!mustBePrimitive || !sym.type.isPrimitive()) {
				output.add(sym);
				if (!analyzingphase) {
					if (AnalysisUtil.isInnerField(defs, sym)) {
						if (!AnalysisUtil.immute_type(sym.type)) {
							Symbol curr_sym = curr.sym;
							log.useSource(sym.outermostClass().sourcefile);
							log.warning(tree.pos(), "confinement.violation",
								sym,
								AnalysisUtil.rmDollar(capsule.sym.toString()),
								AnalysisUtil.rmDollar(curr_sym.toString()));
						}
					}
				}
			}
		}
	}

	public class LeakExpressions extends TreeScanner {
		public LeakExpressions(HashSet<Symbol> output) {
			this.output = output;
		}

		public HashSet<Symbol> output;

		public void visitIdent(JCIdent tree) {
			warning(tree, output, false); }

		public void visitSelect(JCFieldAccess tree) {
			warnJCFieldAccess(tree, output, false);
	    }

		public void visitNewClass(JCNewClass tree) {
			new LeakExpressions(output).scan(tree.def);
	    } 
	}
}
