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