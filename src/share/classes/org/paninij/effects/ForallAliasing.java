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
