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

package org.paninij.analysis;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.List;

// This class detects state that will be assigned a new object in every method
// before the state is used, e.g., Z in the example code below.
/* capsule KeyGen {
	int [] Z = null;             // Encryption subkey (userkey derived).
	IntArray calcEncryptKey() {

		Z = new int [52];         // Encryption subkey (user key derived).

		return new IntArray(Z);
	}
} */

public class FreshStateDetection extends TreeScanner {
	public boolean nonLeak = true;

	public final Symbol state;

	public FreshStateDetection(Symbol state) {
		this.state = state;
	}

	public void visitBlock(JCBlock tree) {
		if (!nonLeak) { return; }
		List<JCStatement> trees = tree.stats;
		if (trees != null) {
	        for (List<? extends JCTree> l = trees; l.nonEmpty(); l = l.tail) {
	        	JCTree head = AnalysisUtil.getEssentialTree(l.head);
	            if (head instanceof JCAssign) {
	            	JCAssign jca = (JCAssign)head;
	            	JCExpression lhs = AnalysisUtil.getEssentialExpr(jca.lhs);
	                JCExpression rhs = AnalysisUtil.getEssentialExpr(jca.rhs);

	                if (lhs instanceof JCIdent &&
	                		AnalysisUtil.isNewExpression(rhs)) {
	                	JCIdent jci = (JCIdent)lhs;
	                	if (jci.sym == state) {
	                		return;
	                	}
	                }
	            }
	            scan(head);
	        }
		}
	}

	public void visitIdent(JCIdent tree) {
		if (tree.sym == state) {
			nonLeak = false;
		}
	}
}
