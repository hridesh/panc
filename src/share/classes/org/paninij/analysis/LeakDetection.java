package org.paninij.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import javax.lang.model.element.ElementKind;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.List;

public class LeakDetection {
	public void getExits(JCMethodDecl meth) {
		System.out.println("analyzing meth = " + meth.sym + "\tclass = " +
				meth.sym.enclClass());
		JCBlock body = meth.body;
		System.out.println("end nodes");
		for (JCTree end : body.endNodes) {
			System.out.println("\t" + end);
		}

		System.out.println("exist nodes");
		for (JCTree end : body.exitNodes) {
			System.out.println("\t" + end);
		}
	}	

	private List<JCTree> defs;

	public void intra(JCCapsuleDecl capsule, JCMethodDecl meth) {
		defs = capsule.defs;

		JCBlock body = meth.body;
		assert body != null;

		TreeSet<JWrapper> queue = new TreeSet<JWrapper>();
		HashMap<JCTree, JWrapper> map = new HashMap<JCTree, JWrapper>();
		for (JCTree tree : body.endNodes) {
			JWrapper temp = new JWrapper(tree);
			map.put(tree, temp);
			queue.add(temp);
		}
		for (JCTree tree : body.exitNodes) {
			JWrapper temp = new JWrapper(tree);
			map.put(tree, temp);
			queue.add(temp);
		}

		while (!queue.isEmpty()) {
			JWrapper w = queue.first();
			queue.remove(w);
			JCTree curr = w.tree;

			HashSet<Symbol> preLeak = new HashSet<Symbol>();
			for (JCTree succ : curr.successors) {
				JWrapper temp = map.get(succ);
				if (temp == null) {
					temp = new JWrapper(succ);
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
				JCMethodInvocation jcmd = (JCMethodInvocation)curr;
				JCExpression currMeth = jcmd.meth;

				boolean innerCall = false;

				// if the method is not intra capsule method call
				if (currMeth instanceof JCIdent) {
					innerCall = true;
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

				if (!innerCall) { warningList(jcmd.args, preLeak); }
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
					JWrapper temp = map.get(predecessor);
					if (temp == null) {
						temp = new JWrapper(predecessor);
						map.put(predecessor, temp);
					}
					queue.add(temp);
				}
			}
		}
	}

	private static class JWrapper implements Comparable<JWrapper> {
		public boolean first = true;
		public HashSet<Symbol> leakVar = new HashSet<Symbol>();
		public final JCTree tree;

		public JWrapper(JCTree tree) { this.tree = tree; }

		public int compareTo(JWrapper o) {
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
					if (sym.name.toString().compareTo("this")==0) {
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
}