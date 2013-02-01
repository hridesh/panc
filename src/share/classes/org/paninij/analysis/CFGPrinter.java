package org.paninij.analysis;

import java.util.LinkedList;

public class CFGPrinter {
	public void printCFG(CFG cfg) {
		LinkedList<CFGNodeImpl> nodesToProcess =
			new LinkedList<CFGNodeImpl>(cfg.nodesInOrder);

		System.out.println("digraph G {");

		while (!nodesToProcess.isEmpty()) {
			CFGNodeImpl node = nodesToProcess.poll();

			for (CFGNodeImpl next : node.predecessors) {
				System.out.println(nodeText(node) + " -> " + nodeText(next));
			}
		}

		System.out.println("}");
	}

	public String nodeText(CFGNodeImpl node) {
		return "\"" + node.id + " " + node.tree.toString().replace("\"", "\\\"")
			+ (node.lhs ? ", lhs" : "") + "\"";
	}
}