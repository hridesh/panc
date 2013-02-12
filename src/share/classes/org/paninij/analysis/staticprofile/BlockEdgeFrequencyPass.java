package org.paninij.analysis.staticprofile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;

/**
 * 
 This pass calculates basic block and edge frequencies based on the branch
 * probability calculated previously. To calculate the block frequency, sum all
 * predecessors edges reaching the block. If the block is the function entry
 * block define the execution frequency of 1. To calculate edge frequencies,
 * multiply the block frequency by the edge from this block to it's successors.
 * 
 * To avoid cyclic problem, this algorithm propagate frequencies to edges by
 * calculating a cyclic probability. More information can be found in Wu (1994).
 * 
 * References: Youfeng Wu and James R. Larus. Static branch frequency and
 * program profile analysis. In MICRO 27: Proceedings of the 27th annual
 * international symposium on Microarchitecture. IEEE, 1994.
 * 
 * @author gupadhyaya
 * 
 */
public class BlockEdgeFrequencyPass {

	// For loops that does not terminates, the cyclic_probability can have a
	// probability higher than 1.0, which is an undesirable condition. Epsilon
	// is used as a threshold of cyclic_probability, limiting its use below 1.0.
	static double epsilon = 0.000001;

	private LoopInfo li;
	private BranchPredictionPass bpp;

	// List of basic blocks not visited.
	Vector<JCTree> notVisited = new Vector<JCTree>();
	// List of loops visited
	private Set<Loop> loopsVisited = new HashSet<Loop>();
	// Hold probabilities propagated to back edges
	HashMap<Pair<JCTree, JCTree>, Double> backEdgesProbabilities = new HashMap<Pair<JCTree, JCTree>, Double>();
	// Block and edge frequency information map
	HashMap<Pair<JCTree, JCTree>, Double> edgeFrequencies = new HashMap<Pair<JCTree, JCTree>, Double>();
	HashMap<JCTree, Double> blockFrequencies = new HashMap<JCTree, Double>();

	public BlockEdgeFrequencyPass(LoopInfo li) {
		this.li = li;
		this.bpp = new BranchPredictionPass(this.li);
		runOnFunction();
	}

	private final boolean runOnFunction() {
		clear();
		// Find all loop headers of this function
		Iterator<Loop> loopsIter = li.iterator();
		while (loopsIter.hasNext()) {
			Loop loop = loopsIter.next();
			propagateLoop(loop);
		}
		// After calculating frequencies for all the loops, calculate the
		// frequencies
		// for the remaining blocks by faking a loop for the function. Assuming
		// that
		// the entry block of the function is a loop head, propagate
		// frequencies.
		// Propagate frequencies assuming entry block is a loop head.
		JCTree entry = li.first();
		markReachable(entry);
		propagateFreq(entry);
		// Clean up unnecessary information.
		notVisited.clear();
		loopsVisited.clear();
		backEdgesProbabilities.clear();
		return false;
	}

	private final void propagateLoop(Loop loop) {
		// Check if we already processed this loop
		if (loopsVisited.contains(loop))
			return;
		// Mark the loop as visited
		loopsVisited.add(loop);
		// Find the most inner loops and process them first
		Iterator<Loop> innerIter = li.inner(loop);
		while (innerIter.hasNext()) {
			Loop inner = innerIter.next();
			propagateLoop(inner);
		}
		// Find the header
		JCTree head = loop.getHead();
		// Get the loop block to which the head belongs to
		// TODO: check if this is not needed, JCTree loopHeadBlock =
		// loop.getHead();
		// Mark as not visited all blocks reachable from the loop head
		markReachable(head);
		// Propagate frequencies from the loop head
		propagateFreq(head);
	}

	/**
	 * MarkReachable - Mark all blocks reachable from root block as not visited
	 * 
	 * @param root
	 */
	private final void markReachable(JCTree root) {
		// Clear the list first
		notVisited.clear();
		// Use an artificial stack
		Stack<JCTree> stack = new Stack<JCTree>();
		stack.push(root);
		// Visit all childs marking them as visited in depth-first order
		while (!stack.empty()) {
			JCTree block = stack.pop();
			if (notVisited.contains(block))
				continue;
			notVisited.add(block);
			// Put the new successors into the stack
			// LoopInfo li = bpp.getBPI().getLoopInformation();
			List<JCTree> successors = block.getSuccessors();
			for (Iterator<JCTree> succIter = successors.iterator(); succIter
					.hasNext();) {
				stack.push(succIter.next());
			}
		}
	}

	/**
	 * PropagateFreq - Compute basic block and edge frequencies by propagating
	 * frequencies
	 * 
	 * @param head
	 */
	private final void propagateFreq(JCTree head) {
		BranchPredictionInfo bpi = bpp.getBPI();
		// Use an artificial stack to avoid recursive calls to PropagateFreq
		Stack<JCTree> stack = new Stack<JCTree>();
		stack.push(head);
		// notVisited.add(head);
		do {
			JCTree basicBlk = stack.pop();
			// If BB has been visited
			if (!(notVisited.contains(basicBlk)))
				continue;
			// Define the block frequency. If it's a loop head, assume it
			// executes only once
			blockFrequencies.put(basicBlk, new Double(1.0f));
			// If it is not a loop head, calculate the block frequencies by
			// summing all
			// edge frequencies reaching this block. If it contains back edges,
			// take
			// into consideration the cyclic probability
			if (!basicBlk.toString().equals(head.toString())) {
				// We can't calculate the block frequency if there is a back
				// edge still
				// not calculated
				boolean invalidEdge = false;
				List<JCTree> predecessors = basicBlk.getPredecessors();
				for (Iterator<JCTree> predIter = predecessors.iterator(); predIter
						.hasNext();) {
					JCTree pred = predIter.next();
					if (notVisited.contains(pred)
							&& !bpi.isBackEdge(new Pair<JCTree, JCTree>(pred,
									basicBlk))) {
						invalidEdge = true;
						// notVisited.remove(basicBlk);
						break;
					}
				}
				// There is an unprocessed predecessor edge
				if (invalidEdge)
					continue;
				// Sum the incoming frequencies edges for this block. Updated
				// the cyclic probability for back edges predecessors
				double bfreq = 0.0;
				double cyclic_probability = 0.0;
				// Verify if BB is a loop head.
				boolean isloopHead = li.isLoopHeader(basicBlk);
				// Calculate the block frequency and the cyclic_probability in
				// case
				// of back edges using the sum of their predecessor's edge
				// frequencies.
				for (Iterator<JCTree> predIter = predecessors.iterator(); predIter
						.hasNext();) {
					JCTree pred = predIter.next();
					Pair<JCTree, JCTree> edge = new Pair<JCTree, JCTree>(
							pred, basicBlk);
					if (bpi.isBackEdge(edge) && isloopHead)
						cyclic_probability += (/*
												 * getProbability(basicBlk,
												 * pred) *
												 */getBackEdgeProbabilities(edge));
					else
						bfreq += getEdgeFrequency(edge);// edgeFrequencies.get(edge);
				}
				// For loops that seems not to terminate, the cyclic probability
				// can be
				// higher than 1.0. In this case, limit the cyclic probability
				// below 1.0.
				if (cyclic_probability > (1.0 - epsilon))
					cyclic_probability = 1.0 - epsilon;
				// Calculate the block frequency.
				blockFrequencies.put(basicBlk, bfreq
						/ (1.0 - cyclic_probability));
			}
			// Mark the block as visited.
			notVisited.remove(basicBlk);

			// Calculate the edges frequencies for all successor of this block.
			List<JCTree> successors = basicBlk.getSuccessors();
			for (Iterator<JCTree> succIter = successors.iterator(); succIter
					.hasNext();) {
				JCTree successor = succIter.next();
				Pair<JCTree, JCTree> edge = new Pair<JCTree, JCTree>(
						basicBlk, successor);
				double prob = bpp.getEdgeProbability(edge);

				/*
				 * // Extra check to make the integrity sound Pair<Block, Block>
				 * backedge = new Pair<Block, Block> (basicBlk, successor); if
				 * ((successors.size() == 1) && bpi.isBackEdge(backedge)) prob =
				 * 1.0;
				 */
				// The edge frequency is the probability of this edge times the
				// block
				// frequency
				double efreq = prob * blockFrequencies.get(basicBlk);
				edgeFrequencies.put(edge, efreq);
				// If a successor is the loop head, update back edge
				// probability.
				if (successor == head)
					backEdgesProbabilities.put(edge, efreq);
			}

			// Propagate frequencies for all successor that are not back edges.
			Vector<JCTree> backEdges = new Vector<JCTree>();
			for (Iterator<JCTree> succIter = successors.iterator(); succIter
					.hasNext();) {
				JCTree successor = succIter.next();
				Pair<JCTree, JCTree> edge = new Pair<JCTree, JCTree>(
						basicBlk, successor);
				if (!bpi.isBackEdge(edge))
					backEdges.add(successor);
			}
			// This was done just to ensure that the algorithm would process the
			// left-most child before, to simulate normal PropagateFreq
			// recursive calls.
			// Forgot to reverse the backedge list
			ListIterator<JCTree> backEdgeListIter = backEdges.listIterator();
			while (backEdgeListIter.hasNext())
				backEdgeListIter.next();

			while (backEdgeListIter.hasPrevious()) {
				JCTree block = backEdgeListIter.previous();
				if (!stack.contains(block))
					stack.push(block);
			}
			/*
			 * for (Iterator<Block> backEdgeIter = backEdges.iterator();
			 * backEdgeIter.hasNext();) { Block unit = backEdgeIter.next(); if
			 * (!stack.contains(unit)) stack.push(unit); }
			 */

		} while (!stack.empty());
	}

	/**
	 * VerifyIntegrity - The sum of frequencies of all edges leading to terminal
	 * nodes should match the entry frequency (that is always 1.0).
	 * 
	 * @param method
	 * @return
	 */
	private boolean VerifyIntegrity(JCMethodDecl method) {
		// The sum of all predecessors edge frequencies.
		double freq = 0.0;
		// If the function has only one block, then the input frequency matches
		// automatically the output frequency.
		if (li.methodBody().size() == 1)
			return true;
		// Find all terminator nodes.
		Iterator<JCStatement> blockIter = li.methodBody().iterator();
		while (blockIter.hasNext()) {
			JCTree block = blockIter.next();
			// If the basic block has no successors, then it s a termination
			// node.
			// List<Unit> successors = LI.getFullSuccessors(block);

			if (block.getSuccessors().size() != 0) {
				// Find all predecessor edges leading to BB.
				List<JCTree> predecessors = block.getPredecessors();
				for (Iterator<JCTree> predIter = predecessors.iterator(); predIter
						.hasNext();) {
					JCTree pred = predIter.next();
					// Sum the predecessors edge frequency.
					freq += getEdgeFrequency(new Pair<JCTree, JCTree>(pred,
							block));
				}
			}

		}
		// Check if frequency matches 1.0 (with a 0.01 slack).
		return (freq > 0.99 && freq < 1.01);
	}

	public double getBackEdgeProbabilities(Pair<JCTree, JCTree> edge) {
		return backEdgesProbabilities.containsKey(edge) ? backEdgesProbabilities
				.get(edge) : bpp.getEdgeProbability(edge);
	}

	public double getEdgeFrequency(Pair<JCTree, JCTree> edge) {
		return edgeFrequencies.containsKey(edge) ? edgeFrequencies.get(edge)
				: 0.0;
	}

	/**
	 * Clear - Clear all stored information.
	 */
	private final void clear() {
		notVisited.clear();
		loopsVisited.clear();
		backEdgesProbabilities.clear();
		edgeFrequencies.clear();
		blockFrequencies.clear();
	}
}
