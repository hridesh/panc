package org.paninij.effects;

import com.sun.tools.javac.tree.*;

import java.util.HashMap;
import java.util.LinkedList;

/* The data structure that represents the control flow graph (CFG). */
public class CFG {
    private HashMap<JCTree, ASTChainNode> nodes =
    	new HashMap<JCTree, ASTChainNode>();
    public ASTChainNode startNode;
    public HeapRepresentation endHeapRepresentation;
    public LinkedList<ASTChainNode> nodesInOrder =
    	new LinkedList<ASTChainNode>();

    public ASTChainNode nodeForTree(JCTree tree) { return nodes.get(tree); }
    
    public void add(ASTChainNode n) {
    	nodes.put(n.tree, n);
    	nodesInOrder.add(n);
    }
}