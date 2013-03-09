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

	public void inter(JCCapsuleDecl capsule, Log log) {
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

		for (Symbol meth : result.keySet()) {
			if ((meth.flags_field & Flags.PRIVATE) != 0) {
				for (Symbol l : result.get(meth)){
					if (l.getKind() == ElementKind.FIELD) {
						log.useSource (capsule.sym.sourcefile);
						log.warning(capsule.pos(), "confinement.violation", l,
							capsule.sym.toString().substring(0,
								capsule.sym.toString().indexOf("$")),
								meth.toString().substring(0,
									capsule.sym.toString().indexOf("$") + 1));
					}
				}
			}
		}
	}

	public HashSet<Symbol> intra(JCCapsuleDecl capsule, JCMethodDecl meth) {
		defs = capsule.defs;

		JCBlock body = meth.body;
		assert body != null;

		TreeSet<TreeWrapper> queue = new TreeSet<TreeWrapper>();
		HashMap<JCTree, TreeWrapper> map = new HashMap<JCTree, TreeWrapper>();
		for (JCTree tree : body.endNodes) {
			TreeWrapper temp = new TreeWrapper(tree);
			map.put(tree, temp);
			queue.add(temp);
		}
		for (JCTree tree : body.exitNodes) {
			TreeWrapper temp = new TreeWrapper(tree);
			map.put(tree, temp);
			queue.add(temp);
		}

		while (!queue.isEmpty()) {
			TreeWrapper w = queue.first();
			queue.remove(w);
			JCTree curr = w.tree;

			HashSet<Symbol> preLeak = new HashSet<Symbol>();
			for (JCTree succ : curr.successors) {
				TreeWrapper temp = map.get(succ);
				if (temp == null) {
					temp = new TreeWrapper(succ);
					map.put(succ, temp);
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

			if (!preLeak.equals(previous) || w.first) {
				w.leakVar.addAll(preLeak);
				w.first = false;
				for (JCTree predecessor : curr.predecessors) {
					TreeWrapper temp = map.get(predecessor);
					if (temp == null) {
						temp = new TreeWrapper(predecessor);
						map.put(predecessor, temp);
					}
					queue.add(temp);
				}
			}
		}

		HashSet<Symbol> intraResult = new HashSet<Symbol>();
		for (JCTree jct : map.keySet()) {
			TreeWrapper jw = map.get(jct);
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