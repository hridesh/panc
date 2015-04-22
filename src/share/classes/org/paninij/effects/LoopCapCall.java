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

import java.util.HashMap;
import java.util.HashSet;

import org.paninij.analysis.AnalysisUtil;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;

public class LoopCapCall extends TreeScanner {
	public HashMap<JCTree, HashSet<Integer>> non_recursive =
		new HashMap<JCTree, HashSet<Integer>>();

	public void visitForLoop(JCForLoop that) {
		super.visitForLoop(that);
		LoopTarget lt = new LoopTarget();
		that.body.accept(lt);

		if (!lt.targets.isEmpty()) {
			for (LoopCapsuleTarget at : lt.targets) {
				LoopCapsuleTarget lct = matching_index(that, at);
				if (lct != null) {
					HashSet<Integer> hs = non_recursive.get(that);
					if (hs == null) {
						hs = new HashSet<Integer>();
						non_recursive.put(that, hs);
					}
					hs.add(lct.pos);
				}
			}
		}
	}

	private static LoopCapsuleTarget matching_index (JCTree that,
			LoopCapsuleTarget at) {
		if (that instanceof JCForLoop) {
			JCForLoop jcf = (JCForLoop)that;
	        HashSet<Symbol> vars = new HashSet<Symbol>();
	        AnalysisUtil.add_loop_index(jcf, vars);

			if (vars.contains(at.index)) {
				return at;
			}
		}
		return null;
	}

	public static class LoopCapsuleTarget {
		public final Symbol index;

		public final JCExpression capsule_instance;
		public final Symbol capsule_meth;

		public final int pos;

		public LoopCapsuleTarget(Symbol index, JCExpression capsule_instance,
				Symbol capsule_meth, int pos) {
			this.index = index;
			this.capsule_instance = capsule_instance;
			this.capsule_meth = capsule_meth;
			this.pos = pos;
		}

		public final String printStr() {
			return capsule_instance + "." + capsule_meth + "(" + pos + ")";
		}
	}

	public static class LoopTarget extends TreeScanner {
		public final HashSet<LoopCapsuleTarget> targets =
			new HashSet<LoopCapsuleTarget>();

		public void visitApply(JCMethodInvocation tree) {			
			JCExpression meth = AnalysisUtil.getEssentialExpr(tree.meth);
			if (meth instanceof JCFieldAccess) {
				JCFieldAccess jcfa = (JCFieldAccess) meth;
				JCExpression selected =
					AnalysisUtil.getEssentialExpr(jcfa.selected);
				if (selected instanceof JCArrayAccess) {
					JCArrayAccess jcaa = (JCArrayAccess)selected;
					JCExpression indexed =
						AnalysisUtil.getEssentialExpr(jcaa.indexed);

					ArrayType at = (ArrayType)indexed.type;
					if (at.elemtype.tsym.isCapsule()) {
						JCExpression index =
							AnalysisUtil.getEssentialExpr(jcaa.index);
						if (index instanceof JCIdent) {
							JCIdent jci = (JCIdent)index;

							int pos = tree.getPreferredPosition();
							targets.add(new LoopCapsuleTarget(jci.sym, jcaa,
									jcfa.sym, pos));
						}
					}
				}
			}
		}
	}
}
