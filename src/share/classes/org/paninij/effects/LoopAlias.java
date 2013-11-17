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

package org.paninij.effects;

import java.util.*;

import javax.tools.JavaFileObject;

import org.paninij.analysis.AnalysisUtil;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.DiagnosticSource;

// For now we only do pattern matching for the limited form of the capsule call
// recognition of the form:
// for (int i = 0; i < capsule_instance.length; i++) {
//   future[i] = capsule_instance[i].capsule_method(k);
// }

public class LoopAlias extends TreeScanner {
	public final JavaFileObject sourcefile;
	public LoopAlias(JavaFileObject sourcefile) {
		this.sourcefile = sourcefile;
	}

	public static class LoopCapsuleTarget {
		public final JCArrayAccess target;
		public final Symbol index;

		public final JCExpression capsule_instance;
		public final Symbol capsule_meth;

		public final int pos;
		public final int line;

		public LoopCapsuleTarget(JCArrayAccess target, Symbol index,
				JCExpression capsule_instance, Symbol capsule_meth, int pos,
				int line) {
			this.target = target;
			this.index = index;
			this.capsule_instance = capsule_instance;
			this.capsule_meth = capsule_meth;
			this.pos = pos;
			this.line = line;
		}

		public final String printStr() {
			return target + "[" + index + "] = " + capsule_instance + "." +
			capsule_meth + "(" + line + ", " + pos + ")";
		}
	}

	public HashMap<JCTree, HashSet<LoopCapsuleTarget>> loop_capsules =
		new HashMap<JCTree, HashSet<LoopCapsuleTarget>>();

	private static LoopCapsuleTarget matching_index (JCTree that,
			LoopCapsuleTarget at) {
		if (that instanceof JCForLoop) {
			JCForLoop jcf = (JCForLoop)that;
	        HashSet<Symbol> vars = new HashSet<Symbol>();
	        AnalysisUtil.add_loop_index(jcf, vars);

			JCExpression index = AnalysisUtil.getEssentialExpr(at.target.index);
			if (index instanceof JCIdent) {
				Symbol sym = ((JCIdent) index).sym;
				if (vars.contains(sym)) {
					return at;
				}
			}
		}
		return null;
	}

	private final void process_targets (JCTree that) {
		LoopTarget lt = new LoopTarget(sourcefile);
		that.accept(lt);

		if (!lt.targets.isEmpty()) {
			for (LoopCapsuleTarget at : lt.targets) {
				LoopCapsuleTarget lct = matching_index(that, at);
				if (lct != null) {
					if (lt.index_vars.contains(lct.index)) {
						HashSet<LoopCapsuleTarget> hs = loop_capsules.get(that);
						if (hs == null) {
							hs = new HashSet<LoopCapsuleTarget>();
							loop_capsules.put(that, hs);
						}
						hs.add(lct);
					}
				}
			}
		}
	}

	final private static void print_targets(JCTree that) {}

	private static void add(HashMap<JCExpression, ArrayList<JCExpression>> scs,
			JCExpression e1, JCExpression e2) {
		ArrayList<JCExpression> temp = scs.get(e1);
		if (temp == null) {
			temp = new ArrayList<JCExpression>();
			scs.put(e1, temp);
		}
		temp.add(e2);
	}

	private static HashSet<Integer> get_index(JCExpression target,
			HashMap<JCExpression, ArrayList<JCExpression>> scs) {
		HashSet<Integer> results = new HashSet<Integer>();
		Object[] os = scs.keySet().toArray();
		for (int i = 0; i < os.length; i++) {
			if (scs.get(os[i]).contains(target)) {
				results.add(i);
			}
		}
		return results;
	}

	private static String change_target(JCExpression target, int sc) {
		JCArrayAccess jcaa = (JCArrayAccess) target;
		return jcaa.indexed + "[sc" + sc + "]";
	}

	// TODO(yuhenglong): for now we only do pattern matching for the for loops.
	public void visitForLoop(JCForLoop that) {
		super.visitForLoop(that);
		print_targets(that);
		process_targets(that);
	}

	public static class LoopTarget extends TreeScanner {
		public final HashSet<LoopCapsuleTarget> targets =
			new HashSet<LoopCapsuleTarget>();
		public final HashSet<Symbol> index_vars = new HashSet<Symbol>();
		public final JavaFileObject sourcefile;

		public LoopTarget(JavaFileObject sourcefile) {
			this.sourcefile = sourcefile;
		}

		public static final LoopCapsuleTarget foreallCall(JCExpression tree,
				JCArrayAccess lhs, JavaFileObject sourcefile) {
			if (tree instanceof JCMethodInvocation) {
				JCExpression meth = ((JCMethodInvocation) tree).meth;
				if (meth instanceof JCFieldAccess) {
					JCFieldAccess jcfa = (JCFieldAccess) meth;
					JCExpression selected = jcfa.selected;
					if (selected instanceof JCArrayAccess) {
						JCArrayAccess jcaa = (JCArrayAccess)selected;
						JCExpression indexed =
							AnalysisUtil.getEssentialExpr(jcaa.indexed);

						ArrayType at = (ArrayType)indexed.type;
						if (at.elemtype.tsym.isCapsule()) {
							JCExpression index0 =
								AnalysisUtil.getEssentialExpr(lhs.index);
							JCExpression index1 =
								AnalysisUtil.getEssentialExpr(jcaa.index);
							if (index0 instanceof JCIdent &&
									index1 instanceof JCIdent) {
								JCIdent jci0 = (JCIdent)index0;
								JCIdent jci1 = (JCIdent)index1;
								if (jci0.sym == jci1.sym) {
									DiagnosticSource ds = new DiagnosticSource(
											sourcefile, null);
									int pos = tree.getPreferredPosition();
									return new LoopCapsuleTarget(lhs, jci0.sym,
											jcaa, jcfa.sym, pos,
											ds.getLineNumber(pos));
								}
							}
						}
					}
				}
			}
			return null;
		}

		public void visitAssign(JCAssign that) {
			JCExpression lhs = AnalysisUtil.getEssentialExpr(that.lhs);
			JCExpression rhs = AnalysisUtil.getEssentialExpr(that.rhs);
			scan(lhs);
			scan(rhs);

			if (lhs instanceof JCArrayAccess) {
				LoopCapsuleTarget at =
					foreallCall(rhs, (JCArrayAccess)lhs, sourcefile);
				if (at != null) {
					targets.add(at);
				}
			}

			if (lhs instanceof JCIdent && AnalysisUtil.int_type(lhs.type)) {
				index_vars.add(((JCIdent) lhs).sym);
			}
		}

		public void visitAssignop(JCAssignOp that) {
			JCExpression lhs = that.lhs;
			scan(lhs);
			scan(that.rhs);

			if (lhs instanceof JCIdent) {
				index_vars.add(((JCIdent) lhs).sym);
			}
		}

		public void visitUnary(JCUnary that) {
			JCExpression arg = AnalysisUtil.getEssentialExpr(that.arg);
			scan(arg);

			if (arg instanceof JCIdent && AnalysisUtil.int_type(arg.type)) {
				Tag opcode = that.getTag();
				if (opcode == JCTree.Tag.PREINC) {
					index_vars.add(((JCIdent) arg).sym);
				} else if (opcode == JCTree.Tag.PREDEC) {
					index_vars.add(((JCIdent) arg).sym);
				} else if (opcode == JCTree.Tag.POSTINC) {
					index_vars.add(((JCIdent) arg).sym);
				} else if (opcode == JCTree.Tag.POSTDEC) {
					index_vars.add(((JCIdent) arg).sym);
				}
			}
		}
	}

	public static final boolean equal_expr(JCExpression t1, JCExpression t2) {
		if (t1 == null && t2 == null) { return true; }
		if (t1 == null && t2 != null) { return false; }
		if (t1 != null && t2 == null) { return false; }

		if (t1 instanceof JCIdent && t2 instanceof JCIdent) {
			JCIdent jci1 = (JCIdent)t1;
			JCIdent jci2 = (JCIdent)t2;

			return jci1.sym == jci2.sym;
		}

		if (t1 instanceof JCUnary && t2 instanceof JCIdent) {
			JCUnary jcu = (JCUnary)t1;
			JCIdent jci = (JCIdent)t2;
			Tag opcode = jcu.getTag();

			if (opcode == JCTree.Tag.PREINC || opcode == JCTree.Tag.PREDEC ||
					opcode == JCTree.Tag.POSTINC ||
					opcode == JCTree.Tag.POSTDEC) {
				return equal_expr(jci, jcu.arg);
			}	
		}

		return false;
	}
}
