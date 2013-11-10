package org.paninij.effects;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.*;

public class ForallAliasing {
	public final JCExpression start;
	public final JCExpression end;
	public final JCExpression stride;
	public final JCExpression array_indexed;
	public final JCExpression capsule_indexed;
	public final Symbol capsule_meth;
	public final int pos;
	public final int line;

	public String printString() {
		return "for all sc in [" + start + ", " + end + ") " +
		array_indexed + "[sc] = " + capsule_indexed + "[sc]." + capsule_meth +
		"(" + pos + ", " + line + ")";
	}

	public ForallAliasing(JCExpression start, JCExpression end,
			JCExpression stride, JCExpression array_indexed,
			JCExpression capsule_indexed, Symbol capsule_meth, int pos,
			int line) {
		this.start = start;
		this.end = end;
		this.stride = stride;
		this.array_indexed = array_indexed;
		this.capsule_indexed = capsule_indexed;
		this.capsule_meth = capsule_meth;
		this.pos = pos;
		this.line = line;
	}
}
