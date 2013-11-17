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

import com.sun.tools.javac.tree.*;

import java.util.HashMap;
import java.util.LinkedList;

/* The data structure that represents the control flow graph (CFG). */
public class CFG {
	private HashMap<JCTree, CFGNodeImpl> nodes = new HashMap<JCTree, CFGNodeImpl>();
	public CFGNodeImpl startNode;
	public LinkedList<CFGNodeImpl> nodesInOrder = new LinkedList<CFGNodeImpl>();

	public CFGNodeImpl nodeForTree(JCTree tree) { return nodes.get(tree); }

	public void add(CFGNodeImpl n) {
		nodes.put(n.tree, n);
		nodesInOrder.add(n);
	}
}