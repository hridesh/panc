package org.paninij.analysis.staticprofile;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCStatement;

public class BranchPredictionPass {
	private BranchPredictionInfo bpi;
	private BranchHeuristicsInfo bhi;
	LoopInfo li;

	HashMap<Pair<JCTree, JCTree>, Double> edgeProbabilities = new HashMap<Pair<JCTree, JCTree>, Double>();

	public BranchPredictionPass(LoopInfo li) {
		this.li = li;
		this.bpi = new BranchPredictionInfo(this.li);
		this.bhi = new BranchHeuristicsInfo(this.li, this.bpi);
		init();
	}

	private final void init() {
		Iterator<JCStatement> nodesToProcess = li.methodBody().iterator();
		while (nodesToProcess.hasNext()) {
			JCTree node = nodesToProcess.next();
			calculateBranchProbabilities(node);
		}
	}

	/**
	 * Implementation of the algorithm proposed by Wu (1994) to calculate the
	 * probabilities of all the successors of a basic block
	 * 
	 * @param block
	 */
	private final void calculateBranchProbabilities(JCTree block) {
		List<JCTree> successors = block.getSuccessors();
		int noOfSuccessors = successors.size();
		int noOfBackEdges = bpi.CountBackEdges(block);
		// The basic block should have some succesors to compute profile
		if (noOfSuccessors != 0) {
			// If a block calls exit, then assume that every successor of this
			// basic block is never going to be reached.
			if (bpi.CallsExit(block)) {
				// According to the paper, successors that contains an exit call have a
				// probability of 0% to be taken.
				for (Iterator<JCTree> succIter = successors.iterator(); succIter.hasNext();) {
					JCTree successorBlk = succIter.next();
					Pair<JCTree, JCTree> edge = new Pair<JCTree, JCTree> (block, successorBlk);
					edgeProbabilities.put(edge, new Double(0.0f));
				}
			} else if (noOfBackEdges > 0 && noOfBackEdges <= noOfSuccessors) {
				// Has some back edges, but not all.
				for (Iterator<JCTree> succIter = successors.iterator(); succIter.hasNext();) {
					JCTree successorBlk = succIter.next();
					Pair<JCTree, JCTree> edge = new Pair<JCTree, JCTree> (block, successorBlk);
					if (bpi.isBackEdge(edge)) {
						float probabilityTaken = bhi.getProbabilityTaken(
								BranchHeuristics.LOOP_BRANCH_HEURISTIC) / noOfBackEdges;
						edgeProbabilities.put(edge, new Double(probabilityTaken));
					} else {
						// The other edge, the one that is not a back edge, is in most cases
						// an exit edge. However, there are situations in which this edge is
						// an exit edge of an inner loop, but not for the outer loop. So,
						// consider the other edges always as an exit edge.
						float probabilityNotTaken = bhi.getProbabilityNotTaken(
								BranchHeuristics.LOOP_BRANCH_HEURISTIC) / (noOfSuccessors - noOfBackEdges);
						edgeProbabilities.put(edge, new Double(probabilityNotTaken));
					}
				}
			} else if (noOfBackEdges > 0 || noOfSuccessors != 2) {
				// This part handles the situation involving switch statements.
				// Every switch case has a equal likelihood to be taken.
				// Calculates the probability given the total amount of cases clauses.
				for (Iterator<JCTree> succIter = successors.iterator(); succIter.hasNext();) {
					JCTree successorBlk = succIter.next();
					Pair<JCTree, JCTree> edge = new Pair<JCTree, JCTree> (block, successorBlk);
					float probability = 1.0f / (float) noOfSuccessors;
					edgeProbabilities.put(edge, new Double(probability));
				}
			} else {
				// Here we can only handle basic blocks with two successors (branches).
				// Identify the two branch edges.
				JCTree trueSuccessor = successors.get(0);
				JCTree falseSuccessor = successors.get(1);

				Pair<JCTree, JCTree> trueEdge = new Pair<JCTree, JCTree> (block, trueSuccessor);
				Pair<JCTree, JCTree> falseEdge = new Pair<JCTree, JCTree> (block, falseSuccessor);

				// Initial branch probability. If no heuristic matches, than each edge
				// has a likelihood of 50% to be taken.
				edgeProbabilities.put(trueEdge, new Double(0.5f));
				edgeProbabilities.put(falseEdge, new Double(0.5f));

				// Run over all heuristics implemented in BranchHeuristics class.
				for (int bh = 0; bh < 9; bh++) {
					Pair<JCTree, JCTree> prediction = bhi.MatchHeuristic(bh, block);

					// Heuristic did not match
					if (prediction == null)
						continue;

					// Recalculate edge probability
					if (prediction.first() != null)
						addEdgeProbability (bh, block, prediction);
				}
			}
		}
	}
	
	/**
	 * getEdgeProbability - Find the edge probability. If the edge is not
	 * found, return 1.0 (probability of 100% of being taken).
	 */
	public Double getEdgeProbability (Pair<JCTree, JCTree> edge) {
		// If edge was found, return it. Otherwise return the default value,
		// meaning that there is no profile known for this edge. The default value
		// is 1.0, meaning that the branch is taken with 100% likelihood.
		
		for (Iterator<Pair<JCTree, JCTree>> edgeIter = edgeProbabilities.keySet().iterator(); edgeIter.hasNext();) {	
			Pair<JCTree, JCTree> stored_edge = edgeIter.next();
			if(stored_edge.toString().equals(edge.toString()))
				return edgeProbabilities.get(stored_edge).doubleValue();
			
		}
		return new Double(1.0f);
	}
	
	/**
	 * addEdgeProbability - If a heuristic matches, calculates the edge
	 * probability combining previous predictions acquired.
	 * @param heuristic
	 * @param root
	 * @param prediction
	 */
	public void addEdgeProbability(int heuristic, JCTree root,
			Pair<JCTree, JCTree> prediction) {
		JCTree successorTaken = prediction.first();
		JCTree successorNotTaken = prediction.second();

		// Get the edges.
		Pair<JCTree, JCTree> edgeTaken = new Pair<JCTree, JCTree> (root, successorTaken);
		Pair<JCTree, JCTree> edgeNotTaken = new Pair<JCTree, JCTree> (root, successorNotTaken);

		// The new probability of those edges.
		Double probTaken = new Double(bhi.getProbabilityTaken(heuristic));
		Double probNotTaken = new Double(bhi.getProbabilityNotTaken(heuristic));
		
		// The old probability of those edges.
		Double oldProbTaken = getEdgeProbability(edgeTaken);
		Double oldProbNotTaken = getEdgeProbability(edgeNotTaken);
		
		// Combined the newly matched heuristic with the already given
		// probability of an edge. Uses the Dempster-Shafer theory to combine
		// probability of two events to occur simultaneously.
		Double d = oldProbTaken * probTaken + oldProbNotTaken * probNotTaken;
		
		edgeProbabilities.put(edgeTaken, (float)(oldProbTaken * probTaken)/d);
		edgeProbabilities.put(edgeNotTaken, (float)(oldProbNotTaken * probNotTaken)/d);
	}
	
	public BranchPredictionInfo getBPI () {
		return bpi;
	}

}
