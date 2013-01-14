package org.paninij.control.flow.graph;

import java.util.LinkedList;

public class CFGPrinter {

    public void printChain(CFG chain) {
        LinkedList<CFGNode> nodesToProcess =
        	new LinkedList<CFGNode>(chain.nodesInOrder);
        
        System.out.println("digraph G {");

        while (!nodesToProcess.isEmpty()) {
            CFGNode node = nodesToProcess.poll();
            
            for (CFGNode next : node.next) {
                System.out.println(nodeText(node) + " -> " + nodeText(next));
            }
        }

        System.out.println("}");
    }

    public String nodeText(CFGNode node) {
        return "\"" + node.id + " " + node.tree.toString().replace("\"", "\\\"")
        	+ (node.lhs ? ", lhs" : "") + "\"";
    }
}