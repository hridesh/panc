package org.paninij.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import javax.lang.model.element.ElementKind;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
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
	private JCMethodDecl currMeth;

	// the intermediate result from the intra procedural analysis.
	private HashMap<JCTree, TreeWrapper> intraMap =
		new HashMap<JCTree, TreeWrapper>();

	public void inter(JCCapsuleDecl capsule, Log log) {
		this.log = log;
		this.capsule = capsule;
		defs = capsule.defs;
		TreeSet<MethodWrapper> queue = new TreeSet<MethodWrapper>();
		HashMap<JCMethodDecl, MethodWrapper> map =
			new HashMap<JCMethodDecl, MethodWrapper>();

		for (JCTree jct : defs) {
			if (jct instanceof JCMethodDecl) {
				if (((JCMethodDecl) jct).body != null) {
					JCMethodDecl meth = (JCMethodDecl)jct;
					MethodWrapper temp = new MethodWrapper(meth);
					map.put(meth, temp);
					queue.add(temp);
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
				if (jcmd.sym.name.toString().contains("$Original")) {
					if ((jcmd.mods.flags & Flags.PRIVATE) != 0) {
						if (jcmd.body != null) {
							intra(capsule, jcmd);
						}
					}
				}
			}
		}
	}

	public HashSet<Symbol> intra(JCCapsuleDecl capsule, JCMethodDecl meth) {
		currMeth = meth;
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
					addCallEdge(meth.sym, jci.sym);

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
					JCExpression receiver = getEssentialExpr(jcfa.selected);

					if (receiver instanceof JCIdent) {
						JCIdent jci = (JCIdent)receiver;
						if (isInnerField(jci.sym)) {
							innerCall = true;
						}
					} else if (receiver instanceof JCFieldAccess) {
						JCFieldAccess field = (JCFieldAccess)receiver;
						if (isInnerField(field.sym)) {
							innerCall = true;
						}
					}
				}

				if (!innerCall) { warningList(jcmi.args, preLeak); }
			} else if (curr instanceof JCAssign) {
				JCAssign jca = (JCAssign)curr;
				JCExpression lhs = getEssentialExpr(jca.lhs);
				if (lhs instanceof JCIdent) {
					JCIdent jci = (JCIdent)lhs;
					if (preLeak.contains(jci.sym)) {
						warningCandidates(jca.rhs, preLeak);
					}
				} else if (lhs instanceof JCFieldAccess) {
					JCFieldAccess jcf = (JCFieldAccess)lhs;

					if (isVarThis(jcf.selected)) {
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

	private void warningCandidates(JCTree tree, HashSet<Symbol> output) {
		if (tree != null) {
			if (tree instanceof JCExpression) {
				JCExpression jce = (JCExpression)tree;
				tree = getEssentialExpr(jce);
			}

			if (tree instanceof JCIdent) {
				warning((JCIdent) tree, output);
			} else if (tree instanceof JCFieldAccess) {
				JCFieldAccess jcfa = (JCFieldAccess)tree;
				if (!jcfa.type.isPrimitive()) {
					output.add(jcfa.sym);
					if (!analyzingphase) {
						JCExpression selected = jcfa.selected;
						if (isVarThis(selected) && isInnerField(jcfa.sym)) {
							if (jcfa.sym.getKind() == ElementKind.FIELD) {
								Symbol capSym = capsule.sym;
								log.useSource (
									jcfa.sym.outermostClass().sourcefile);
								log.warning(tree.pos(), "confinement.violation",
									jcfa.sym, capSym.toString().substring(0,
										capSym.toString().indexOf("$")),
											currMeth.sym.toString().substring(0,
												currMeth.sym.toString().indexOf("$")));
							}
						}
					}
				}
			}
		}
	}

	private static JCExpression getEssentialExpr(JCExpression original) {
		JCExpression rightOp = original;
		while (rightOp instanceof JCTypeCast || rightOp instanceof JCParens) {
			if (rightOp instanceof JCTypeCast) {
				rightOp = ((JCTypeCast)rightOp).expr;
			} else if (rightOp instanceof JCParens) {
				rightOp = ((JCParens)rightOp).expr;
			}
		}
		return rightOp;
	}

	private void warning(JCIdent tree, HashSet<Symbol> output) {
		Symbol sym = tree.sym;
		if (sym != null) {
			if (!sym.type.isPrimitive()) {
				output.add(sym);
				if (!analyzingphase) {
					if (isInnerField(sym) &&
							sym.getKind() == ElementKind.FIELD) {
						log.useSource (sym.outermostClass().sourcefile);
						log.warning(tree.pos(), "confinement.violation",
							sym, capsule.sym.toString().substring(
								0, capsule.sym.toString().indexOf("$")),
									currMeth.sym.toString().substring(
										0, currMeth.sym.toString().indexOf("$")));
					}
				}
			}
		}
	}

	private static boolean isVarThis(JCTree that) {
		if (that instanceof JCIdent) {
			JCIdent tree = (JCIdent)that;
			Symbol sym = tree.sym;
			if (sym != null) {
				ElementKind symKind = sym.getKind();
				if (symKind == ElementKind.FIELD) {
					if (sym.name.toString().compareTo("this") == 0) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isInnerField(Symbol s) {
		for (JCTree def : defs) {
			if (def instanceof JCVariableDecl) {
				JCVariableDecl field = (JCVariableDecl)def;
				if (field.sym == s &&
						((field.mods.flags & Flags.PRIVATE) != 0)) {
					return true;
				}
			}
		}
		return false;
	}

	private void addCallEdge(Symbol caller, Symbol callee) {
		HashSet<Symbol> callers = reversed.get(callee);
		if (callers == null) {
			callers = new HashSet<Symbol>();
			reversed.put(callee, callers);
		}
		callers.add(caller);
	}
}