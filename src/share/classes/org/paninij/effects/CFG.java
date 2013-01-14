package org.paninij.effects;

import com.sun.tools.javac.tree.*;

import java.util.HashMap;
import java.util.LinkedList;

/* The data structure that represents the control flow graph (CFG). */
public class CFG {
    private HashMap<JCTree, CFGNode> nodes =
    	new HashMap<JCTree, CFGNode>();
    public CFGNode startNode;
    public HeapRepresentation endHeapRepresentation;
    public LinkedList<CFGNode> nodesInOrder =
    	new LinkedList<CFGNode>();

    public CFGNode nodeForTree(JCTree tree) { return nodes.get(tree); }
    
    public void add(CFGNode n) {
    	nodes.put(n.tree, n);
    	nodesInOrder.add(n);
    }
}