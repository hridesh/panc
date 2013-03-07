package org.paninij.analysis;

import java.util.List;

/* Data structure that represents the node of the control flow graph. */
public interface CFGNode {
	// return the successors of the current node in the control flow graph
	public List<? extends CFGNode> getSuccessors();

	// return the predecessors of the current node in the control flow graph
	public List<? extends CFGNode> getPredecessors();

    public boolean isLHS();



}