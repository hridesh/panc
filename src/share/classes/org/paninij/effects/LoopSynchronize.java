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

import org.paninij.analysis.AnalysisUtil;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;

// For now we only do pattern matching for the limited form of the capsule call
// recognition of the form:
// for (int i = 0; i < capsule_instance.length; i++) {
//   future[i] = capsule_instance[i].capsule_method(k);
// }

public class LoopSynchronize extends TreeScanner {
	public final HashMap<JCTree, AliasingGraph> alias;
	public LoopSynchronize(HashMap<JCTree, AliasingGraph> alias) {
		this.alias = alias;
	}

	public HashMap<JCTree, HashSet<CollectedCall>> loop_collect =
		new HashMap<JCTree, HashSet<CollectedCall>>();

	private static CollectedCall matching_index (JCTree that,
			CollectedCall at) {
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

	private final void process_targets (JCTree that) {
		java.util.List<JCTree> startNodes = that.startNodes;
		assert startNodes.size() == 1;
		JCTree start = startNodes.get(0);

		HashSet<ForallAliasing> forall_alias = alias.get(start).forall_alias;

		LoopTarget lt = new LoopTarget(forall_alias);
		that.accept(lt);

		if (!lt.collected.isEmpty()) {
			for (CollectedCall at : lt.collected) {
				CollectedCall lct = matching_index(that, at);
				if (lct != null) {
					HashSet<CollectedCall> hs = loop_collect.get(that);
					if (hs == null) {
						hs = new HashSet<CollectedCall>();
						loop_collect.put(that, hs);
					}
					hs.add(lct);
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
		public final HashSet<ForallAliasing> forall_alias;
		public final HashSet<CollectedCall> collected =
			new HashSet<CollectedCall>();

		public LoopTarget(HashSet<ForallAliasing> forall_alias) {
			this.forall_alias = forall_alias;
		}

		public void visitApply(JCMethodInvocation tree) {
	        scan(tree.typeargs);
	        scan(tree.meth);
	        scan(tree.args);

	        JCExpression meth = AnalysisUtil.getEssentialExpr(tree.meth);
	        if (meth instanceof JCFieldAccess) {
	        	JCFieldAccess field = (JCFieldAccess)meth;
	        	JCExpression selected =
	        		AnalysisUtil.getEssentialExpr(field.selected);
	        	if (selected instanceof JCArrayAccess) {
	        		JCArrayAccess array = (JCArrayAccess)selected;
	        		JCExpression indexed =
	        			AnalysisUtil.getEssentialExpr(array.indexed);
	        		JCExpression index =
	        			AnalysisUtil.getEssentialExpr(array.index);
	        		if (indexed instanceof JCIdent &&
	        				index instanceof JCIdent) {
	        			JCIdent jci0 = (JCIdent)indexed;
	        			for (ForallAliasing alias : forall_alias) {
	        				JCExpression array_indexed = alias.array_indexed;
	        				if (array_indexed instanceof JCIdent) {
	        					JCIdent jci1 = (JCIdent)array_indexed;
	        					if (jci1.sym == jci0.sym) {
	        						collected.add(new CollectedCall(jci1.sym,
	        								((JCIdent)index).sym, alias.pos,
	        								field.sym));
	        					}
	        				}
	        			}
	        		}
	        	}
	        }
	    }
	}

	public static class CollectedCall {
		public final Symbol receiver;
		public final Symbol index;
		public final int pos;
		public final Symbol meth;

		public CollectedCall(Symbol receiver, Symbol index, int pos,
				Symbol meth) {
			this.receiver = receiver;
			this.index = index;
			this.pos = pos;
			this.meth = meth;
		}

		public String printStr() {
			return receiver + "[" + index + "]." + meth + "(" + pos + ")";
		}
	}
}
