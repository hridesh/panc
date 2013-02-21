package org.paninij.analysis.staticprofile;

import java.util.List;

import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.Tag;

public class BranchHeuristicsInfo {

	private LoopInfo li;
	private BranchPredictionInfo predInfo;
	private PostDominator pd;

	// There are 9 branch prediction heuristics.
	static final int numBranchHeuristics = 9;
	static BranchProbabilities[] probList = new BranchProbabilities[numBranchHeuristics];

	public BranchHeuristicsInfo(LoopInfo li, BranchPredictionInfo predInfo) {
		this.li = li;
		this.predInfo = predInfo;
		this.pd = pd;
		init();
	}

	/**
	 * Initialize heuristic values The list of all heuristics with their
	 * respective probabilities. Notice that the list respect the order given in
	 * the ProfileHeuristics enumeration. This order will be used to index this
	 * list.
	 */
	private void init() {
		float[] probTaken = { 0.88f, 0.60f, 0.78f, 0.84f, 0.80f, 0.72f, 0.55f,
				0.75f, 0.62f };
		float[] probNotTaken = { 0.12f, 0.40f, 0.22f, 0.16f, 0.20f, 0.28f,
				0.45f, 0.25f, 0.38f };
		String[] desc = { "Loop Branch Heuristic", "Pointer Heuristic",
				"Call Heuristic", "Opcode Heuristic", "Loop Exit Heuristic",
				"Return Heuristic", "Store Heuristic", "Loop Header Heuristic",
				"Guard Heuristic" };
		for (int i = 0; i < 9; i++) {
			probList[i] = new BranchProbabilities(i, probTaken[i],
					probNotTaken[i], desc[i]);
		}
	}

	public float getProbabilityTaken(int heuristic) {
		return probList[heuristic].probabilityTaken;
	}

	public float getProbabilityNotTaken(int heuristic) {
		return probList[heuristic].probabilityNotTaken;
	}

	/**
	 * MatchHeuristic - Wrapper for the heuristics handlers meet above. This
	 * procedure assumes that root basic block has exactly two successors.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 */
	public Pair<JCTree, JCTree> MatchHeuristic(int bh, JCTree root) {
		// Try to match the heuristic bh with their respective handler.
		switch (bh) {
		case BranchHeuristics.LOOP_BRANCH_HEURISTIC:
			return MatchLoopBranchHeuristic(root);
		case BranchHeuristics.POINTER_HEURISTIC:
			return MatchPointerHeuristic(root);
		case BranchHeuristics.CALL_HEURISTIC:
			return MatchCallHeuristic(root);
		case BranchHeuristics.OPCODE_HEURISTIC:
			return MatchOpcodeHeuristic(root);
		case BranchHeuristics.LOOP_EXIT_HEURISTIC:
			return MatchLoopExitHeuristic(root);
		case BranchHeuristics.RETURN_HEURISTIC:
			return MatchReturnHeuristic(root);
		case BranchHeuristics.STORE_HEURISTIC:
			return MatchStoreHeuristic(root);
		case BranchHeuristics.LOOP_HEADER_HEURISTIC:
			return MatchLoopHeaderHeuristic(root);
		case BranchHeuristics.GUARD_HEURISTIC:
			return MatchGuardHeuristic(root);
		default:
			// Unknown heuristic.
			// Should never happen.
			return null;
		}
	}

	/**
	 * MatchLoopBranchHeuristic - Predict as taken an edge back to a loop's
	 * head. Predict as not taken an edge exiting a loop.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 */
	private final Pair<JCTree, JCTree> MatchLoopBranchHeuristic(JCTree root) {
		boolean matched = false;
		Pair<JCTree, JCTree> prediction = null;
		List<JCTree> successors = root.getSuccessors();
		if (successors == null)	return null;
		if (successors.size() < 2)
			return null;
		// Basic block successors, the true and false branches.
		JCTree trueSuccessor = successors.get(0);
		JCTree falseSuccessor = null;
		if (successors.size() > 1)
			falseSuccessor = successors.get(1);
		// true and false edge
		Pair<JCTree, JCTree> falseEdge = new Pair<JCTree, JCTree>(
				falseSuccessor, trueSuccessor);
		Pair<JCTree, JCTree> trueEdge = new Pair<JCTree, JCTree>(
				trueSuccessor, falseSuccessor);
		// If the true branch is a back edge to a loop's head or the false
		// branch is
		// an exit edge, match the heuristic.
		if ((predInfo.isBackEdge(trueEdge) && li.isLoopHeader(trueSuccessor))
				|| predInfo.isExitEdge(falseEdge)) {
			matched = true;
			prediction = new Pair<JCTree, JCTree>(trueSuccessor, falseSuccessor);
		}
		
		if (falseSuccessor != null) {
			// Check the opposite situation, the other branch.
			if ((predInfo.isBackEdge(falseEdge) && li.isLoopHeader(falseSuccessor
					)) || predInfo.isExitEdge(trueEdge)) {
				if (matched)
					return null;
				matched = true;
				prediction = new Pair<JCTree, JCTree>(falseSuccessor, trueSuccessor);
			}
		}
		
		return prediction;
	}

	/**
	 * MatchPointerHeuristic - Predict that a comparison of a pointer against
	 * null or of two pointers will fail.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 */
	private final Pair<JCTree, JCTree> MatchPointerHeuristic(JCTree root) {
		Pair<JCTree, JCTree> prediction = null;
		if (!(root.getTag().equals(Tag.IF)))
			return null;
		List<JCTree> successors = root.getSuccessors();
		if (successors == null)	return null;
		if (successors.size() < 2)
			return null;
		// Basic block successors, the true and false branches.
		JCTree trueSuccessor = successors.get(1);
		JCTree falseSuccessor = null;
		if (successors.size() > 1)
			falseSuccessor = successors.get(0);
		if (((JCIf)root).getCondition().getKind().equals(Kind.EQUAL_TO))
			prediction = new Pair<JCTree, JCTree>(falseSuccessor, trueSuccessor);
		else
			prediction = new Pair<JCTree, JCTree>(trueSuccessor, falseSuccessor);
		return prediction;
	}

	/**
	 * MatchCallHeuristic - Predict a successor that contains a call and does
	 * not post-dominate will not be taken.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 * @param rootBasicBlock
	 */
	private final Pair<JCTree, JCTree> MatchCallHeuristic(JCTree root) {
		boolean matched = false;
		Pair<JCTree, JCTree> prediction = null;
		List<JCTree> successors = root.getSuccessors();
		if (successors == null)	return null;
		if (successors.size() < 2)
			return null;
		// Basic block successors, the true and false branches.
		JCTree trueSuccessor = successors.get(0);
		JCTree falseSuccessor = null;
		if (successors.size() > 1)
			falseSuccessor = successors.get(1);
		// Check if the successor contains a call and does not post-dominate.
		if (predInfo.hasCall(trueSuccessor)
				&& !pd.dominates(trueSuccessor, root)) {
			matched = true;
			prediction = new Pair<JCTree, JCTree>(falseSuccessor, trueSuccessor);
		}

		// Check the opposite situation, the other branch.
		if (falseSuccessor != null) {
			if (predInfo.hasCall(falseSuccessor)
					&& !pd.dominates(falseSuccessor, root)) {

				// If the heuristic matches both branches, predict none.
				if (matched)
					return null;

				matched = true;
				prediction = new Pair<JCTree, JCTree>(trueSuccessor,
						falseSuccessor);
			}
		}
		return prediction;
	}

	/**
	 * MatchOpcodeHeuristic - Predict that a comparison of an integer for less
	 * than zero, less than or equal to zero, or equal to a constant, will fail.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 * @param rootBasicBlock
	 */
	private final Pair<JCTree, JCTree> MatchOpcodeHeuristic(JCTree root) {
		return null;
	}

	/**
	 * MatchLoopExitHeuristic - Predict that a comparison in a loop in which no
	 * successor is a loop head will not exit the loop.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 * @param rootBasicBlock
	 */
	private final Pair<JCTree, JCTree> MatchLoopExitHeuristic(JCTree root) {
		Pair<JCTree, JCTree> prediction = null;
		List<JCTree> successors = root.getSuccessors();
		if (successors == null)	return null;
		if (successors.size() < 2)
			return null;
		// Basic block successors, the true and false branches.
		JCTree trueSuccessor = successors.get(0);
		JCTree falseSuccessor = null;
		if (successors.size() > 1)
			falseSuccessor = successors.get(1);
		Loop loop = li.returnContainingLoop(trueSuccessor);
		// If there's a loop, check if neither of the branches are loop headers.
		if ((loop == null) || loop.getHead().equals(trueSuccessor)
				|| loop.getHead().equals(falseSuccessor))
			return null;
		Pair<JCTree, JCTree> falseEdge = new Pair<JCTree, JCTree>(
				falseSuccessor, trueSuccessor);
		Pair<JCTree, JCTree> trueEdge = new Pair<JCTree, JCTree>(
				trueSuccessor, falseSuccessor);
		// If it is an exit edge, successor will fail so predict the other
		// branch.
		// Note that is not possible for both successors to be exit edges.
		if (predInfo.isExitEdge(trueEdge))
			return new Pair<JCTree, JCTree>(falseSuccessor, trueSuccessor);
		else if (predInfo.isExitEdge(falseEdge))
			return new Pair<JCTree, JCTree>(trueSuccessor, falseSuccessor);

		return null;
	}

	/**
	 * MatchReturnHeuristic - Predict a successor that contains a return will
	 * not be taken.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 * @param rootBasicBlock
	 */
	private final Pair<JCTree, JCTree> MatchReturnHeuristic(JCTree root) {
		boolean matched = false;
		Pair<JCTree, JCTree> prediction = null;
		List<JCTree> successors = root.getSuccessors();
		if (successors == null)	return null;
		if (successors.size() < 2)
			return null;
		// Basic block successors, the true and false branches.
		JCTree trueSuccessor = successors.get(0);
		JCTree falseSuccessor = null;
		if (successors.size() > 1)
			falseSuccessor = successors.get(1);
		// Check if the true successor it's a return instruction.
		if (hasReturn(trueSuccessor)) {
			matched = true;
			prediction = new Pair<JCTree, JCTree>(falseSuccessor, trueSuccessor);
		}

		// Check the opposite situation, the other branch.
		if (hasReturn(falseSuccessor)) {
			// If the heuristic matches both branches, predict none.
			if (matched)
				return null;
			matched = true;
			prediction = new Pair<JCTree, JCTree>(trueSuccessor, falseSuccessor);
		}
		return prediction;
	}
	
	private boolean hasReturn (JCTree node) {
		if (node.getTag().equals(Tag.RETURN))	return true;
		return false;
	}

	/**
	 * MatchStoreHeuristic - Predict a successor that contains a store
	 * instruction and does not post-dominate will not be taken.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 * @param rootBasicBlock
	 */
	private final Pair<JCTree, JCTree> MatchStoreHeuristic(JCTree root) {
		boolean matched = false;
		Pair<JCTree, JCTree> prediction = null;
		List<JCTree> successors = root.getSuccessors();
		if (successors == null)	return null;
		if (successors.size() < 2)
			return null;
		// Basic block successors, the true and false branches.
		JCTree trueSuccessor = successors.get(0);
		JCTree falseSuccessor = null;
		if (successors.size() > 1)
			falseSuccessor = successors.get(1);
		// Check if the successor contains a call and does not post-dominate.
		if (predInfo.hasStore(trueSuccessor)
				&& !pd.dominates(trueSuccessor, root)) {
			matched = true;
			prediction = new Pair<JCTree, JCTree>(falseSuccessor, trueSuccessor);
		}

		// Check the opposite situation, the other branch.
		if (falseSuccessor != null) {
			if (predInfo.hasStore(falseSuccessor)
					&& !pd.dominates(falseSuccessor, root)) {

				// If the heuristic matches both branches, predict none.
				if (matched)
					return null;

				matched = true;
				prediction = new Pair<JCTree, JCTree>(trueSuccessor,
						falseSuccessor);
			}
		}
		return prediction;
	}

	/**
	 * MatchLoopHeaderHeuristic - Predict a successor that is a loop header or a
	 * loop pre-header and does not post-dominate will be taken.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 * @param rootBasicBlock
	 */
	private final Pair<JCTree, JCTree> MatchLoopHeaderHeuristic(JCTree root) {
		boolean matched = false;
		Pair<JCTree, JCTree> prediction = null;
		List<JCTree> successors = root.getSuccessors();
		if (successors == null)	return null;
		if (successors.size() < 2)
			return null;
		// Basic block successors, the true and false branches.
		JCTree trueSuccessor = successors.get(0);
		JCTree falseSuccessor = null;
		if (successors.size() > 1)
			falseSuccessor = successors.get(1);
		// Get the most inner loop in which the true successor basic block is
		// in.
		Loop loop = li.returnContainingLoop(trueSuccessor);

		if ((loop != null)
				&& ((loop.getHead().toString().equals(trueSuccessor.toString()) || loop
						.getPreHeader().equals(trueSuccessor)) && !pd.dominates(
						trueSuccessor, root))) {
			matched = true;
			prediction = new Pair<JCTree, JCTree>(trueSuccessor,
					falseSuccessor);
		}
		
		if (falseSuccessor != null) {
			// Get the most inner loop in which the false successor basic block
			// is
			// in.
			loop = li.returnContainingLoop(falseSuccessor);

			// Check if exists a loop,
			// the false branch successor is a loop header or a loop pre-header,
			// and
			// does not post dominate.
			if ((loop != null)
					&& ((loop.getHead().toString()
							.equals(falseSuccessor.toString()) || li
							.loopPreHeader(falseSuccessor)) && !pd.dominates(
							falseSuccessor, root))) {
				// If the heuristic matches both branches, predict none.
				if (matched)
					return null;
				matched = true;
				prediction = new Pair<JCTree, JCTree>(falseSuccessor,
						trueSuccessor);
			}
		}
		return prediction;
	}

	/**
	 * MatchGuardHeuristic - Predict that a comparison in which a register is an
	 * operand, the register is used before being defined in a successor block,
	 * and the successor block does not post-dominate will reach the successor
	 * block.
	 * 
	 * @returns a Prediction that is a pair in which the first element is the
	 *          successor taken, and the second the successor not taken.
	 * @param rootBasicBlock
	 */
	private final Pair<JCTree, JCTree> MatchGuardHeuristic(JCTree root) {
		if (!(root.getTag().equals(Tag.IF)))
			return null;
		List<JCTree> successors = root.getSuccessors();
		if (successors == null)	return null;
		if (successors.size() == 0)
			return null;
		// Basic block successors, the true and false branches.
		JCTree trueSuccessor = successors.get(0);
		JCTree falseSuccessor = null;
		if (successors.size() > 1)
			falseSuccessor = successors.get(1);
		// true and false edge
		Pair<JCTree, JCTree> falseEdge = new Pair<JCTree, JCTree>(
				falseSuccessor, trueSuccessor);
		Pair<JCTree, JCTree> trueEdge = new Pair<JCTree, JCTree>(
				trueSuccessor, falseSuccessor);
		return null;
	}
}
