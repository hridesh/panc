package org.paninij.analysis;

import java.util.*;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;

public class CommonMethod {
	public static JCExpression essentialExpr(JCExpression original) {
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

	// The following two methods are for constructing the order for the for the
	// analysis for methods.
	public static Collection<JCTree> constructWorklist(ArrayList<JCTree> order) {
		return new TreeSet<JCTree>(new InnerComparator(order));
	}

	private static class InnerComparator implements Comparator<JCTree> {
		public final ArrayList<JCTree> order;

		public InnerComparator(ArrayList<JCTree> order) {
			this.order = order;
		}

		public int compare(JCTree o1, JCTree o2) {
			int i1 = order.indexOf(o1);
			int i2 = order.indexOf(o2);
			return i1 - i2;
		}
	}
}