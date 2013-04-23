package org.paninij.analysis;

import java.util.ArrayList;
import java.util.List;

import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;

/* Data structure that represents the node of the control flow graph. */
public class CFGNodeImpl implements CFGNode {
	public JCTree tree;
	public int id;
	public static int counter = 0;
	public ArrayList<CFGNodeImpl> predecessors = new ArrayList<CFGNodeImpl>();
	public ArrayList<CFGNodeImpl> successors = new ArrayList<CFGNodeImpl>();
	public ArrayList<CFGNodeImpl> startNodes = new ArrayList<CFGNodeImpl>();
	public ArrayList<CFGNodeImpl> endNodes = new ArrayList<CFGNodeImpl>();
	public ArrayList<CFGNodeImpl> excEndNodes = new ArrayList<CFGNodeImpl>();
	public boolean lhs = false;

	// Effects of control flow paths up to and including this node.

	public CFGNodeImpl(JCTree tree) {
		this.tree = tree;
		id = counter++;
	}

	public int hashCode() {
		return tree.hashCode(); 
	}

	public boolean equals(Object o) {
		if (o instanceof CFGNodeImpl){
			CFGNodeImpl g = (CFGNodeImpl)o;
			return tree.equals(g.tree);
		}
		return false;
	}

	public void connectToEndNodesOf(CFGNodeImpl n) {
		for (CFGNodeImpl endNode : n.endNodes) {
			endNode.predecessors.add(this);
			this.successors.add(endNode);
		}
	}

	public void connectToStartNodesOf(CFGNodeImpl n) {
		for (CFGNodeImpl startNode : n.startNodes) {
			startNode.successors.add(this);
			this.predecessors.add(startNode);
		}
	}

	public void connectStartNodesToEndNodesOf(CFGNodeImpl n) {
		for (CFGNodeImpl endNode : n.endNodes) {
			for (CFGNodeImpl startNode : startNodes) {
				endNode.predecessors.add(startNode);
				startNode.successors.add(endNode);
			}
		}
	}

	public void connectStartNodesToContinuesOf(CFGNodeImpl n) {
		for (CFGNodeImpl endNode : n.excEndNodes) {
			if (endNode.tree instanceof JCBreak) {
				throw new Error("should not reach JCBreak");
			} else if (endNode.tree instanceof JCContinue) {
				endNode.predecessors.addAll(n.startNodes);
				for (CFGNodeImpl startNode : n.startNodes) {
					startNode.successors.add(endNode);
				}
			} else if (endNode.tree instanceof JCReturn) {
			} else if (endNode.tree instanceof JCThrow) {
			} else throw new Error("this shouldn't happen");
		}
	}

	public List<CFGNodeImpl> getSuccessors() {
		return successors;
	}

	public List<CFGNodeImpl> getPredecessors() {
		return predecessors;
	}
}