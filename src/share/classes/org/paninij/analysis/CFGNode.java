package org.paninij.analysis;

import java.util.ArrayList;

import org.paninij.effects.EffectSet;
import org.paninij.effects.HeapRepresentation;

import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;

/* Data structure that represents the node of the control flow graph. */
public class CFGNode {
	public JCTree tree;
    public int id;
    public static int counter = 0;
	public ArrayList<CFGNode> predecessors = new ArrayList<CFGNode>();
	public ArrayList<CFGNode> successors = new ArrayList<CFGNode>();
    public ArrayList<CFGNode> startNodes = new ArrayList<CFGNode>();
    public ArrayList<CFGNode> endNodes = new ArrayList<CFGNode>();
    public ArrayList<CFGNode> excEndNodes = new ArrayList<CFGNode>();
    public boolean lhs = false;
    
    // Effects of control flow paths up to and including this node.
    public EffectSet effects = new EffectSet();

    public HeapRepresentation heapRepresentation;

	public CFGNode(JCTree tree) {
		this.tree = tree;
        id = counter++;
	}

	public int hashCode() {
		return tree.hashCode(); 
	}

	public boolean equals(Object o) {
		if (o instanceof CFGNode){
			CFGNode g = (CFGNode)o;
			return tree.equals(g.tree);
		}
		return false;
	}

    public void connectToEndNodesOf(CFGNode n) {
        for (CFGNode endNode : n.endNodes) {
            endNode.predecessors.add(this);
            this.successors.add(endNode);
        }
    }

    public void connectToStartNodesOf(CFGNode n) {
        for (CFGNode startNode : n.startNodes) {
            startNode.successors.add(this);
            this.predecessors.add(startNode);
        }
    }

    public void connectStartNodesToEndNodesOf(CFGNode n) {
        for (CFGNode endNode : n.endNodes) {
            for (CFGNode startNode : startNodes) {
                endNode.predecessors.add(startNode);
                startNode.successors.add(endNode);
            }
        }
    }

    public void connectStartNodesToContinuesOf(CFGNode n) {
        for (CFGNode endNode : n.excEndNodes) {
            if (endNode.tree instanceof JCBreak) {
                throw new Error("should not reach JCBreak");
            } else if (endNode.tree instanceof JCContinue) {
                endNode.predecessors.addAll(n.startNodes);
                for (CFGNode startNode : n.startNodes) {
                    startNode.successors.add(endNode);
                }
            } else if (endNode.tree instanceof JCReturn) {
            } else if (endNode.tree instanceof JCThrow) {
            } else throw new Error("this shouldn't happen");
        }
    }

    public ArrayList<CFGNode> getSuccessors() {
    	return successors;
    }

    public ArrayList<CFGNode> getPredecessors() {
    	return predecessors;
    }
}