package org.paninij.effects;

import java.util.ArrayList;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;

public class ASTChainNode {
	public JCTree tree;
    public int id;
    public static int counter = 0;
	public ArrayList<ASTChainNode> next = new ArrayList<ASTChainNode>();
	public ArrayList<ASTChainNode> previous = new ArrayList<ASTChainNode>();
    public ArrayList<ASTChainNode> startNodes = new ArrayList<ASTChainNode>();
    public ArrayList<ASTChainNode> endNodes = new ArrayList<ASTChainNode>();
    public ArrayList<ASTChainNode> excEndNodes = new ArrayList<ASTChainNode>();
    
    // effects of chain up to and including this node
    public EffectSet effects = new EffectSet();

    public HeapRepresentation heapRepresentation;
    

	public ASTChainNode(JCTree tree) {
		this.tree = tree;
        id = counter++;
	}

	public int hashCode() {
		return tree.hashCode(); 
	}

	public boolean equals(Object o) {
		if (o instanceof ASTChainNode){
			ASTChainNode g = (ASTChainNode)o;
			return tree.equals(g.tree);
		}
		return false;
	}

    public void connectToEndNodesOf(ASTChainNode n) {
        for (ASTChainNode endNode : n.endNodes) {
            endNode.next.add(this);
            this.previous.add(endNode);
        }
    }

    public void connectToStartNodesOf(ASTChainNode n) {
        for (ASTChainNode startNode : n.startNodes) {
            startNode.previous.add(this);
            this.next.add(startNode);
        }
    }

    public void connectStartNodesToEndNodesOf(ASTChainNode n) {
        for (ASTChainNode endNode : n.endNodes) {
            for (ASTChainNode startNode : startNodes) {
                endNode.next.add(startNode);
                startNode.previous.add(endNode);
            }
        }
    }

    public void connectStartNodesToContinuesOf(ASTChainNode n) {
        for (ASTChainNode endNode : n.excEndNodes) {
            if (endNode.tree instanceof JCBreak) {
                throw new Error("should not reach JCBreak");
            } else if (endNode.tree instanceof JCContinue) {
                endNode.next.addAll(n.startNodes);
                for (ASTChainNode startNode : n.startNodes) {
                    startNode.previous.add(endNode);
                }
            } else if (endNode.tree instanceof JCReturn) {
            } else if (endNode.tree instanceof JCThrow) {
            } else throw new Error("this shouldn't happen");
        }
    }
}