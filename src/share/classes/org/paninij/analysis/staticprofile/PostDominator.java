package org.paninij.analysis.staticprofile;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.paninij.analysis.CFG;
import org.paninij.analysis.CFGNode;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCStatement;

/**
 * Calculate dominators for basic blocks.
 * <p>
 * Uses the algorithm contained in Dragon book, pg. 670-1.
 * 
 * <pre>
 *       D(n0) := { n0 }
 *       for n in N - { n0 } do D(n) := N;
 *       while changes to any D(n) occur do
 *         for n in N - {n0} do
 *             D(n) := {n} U (intersect of D(p) over all predecessors p of n)
 * </pre>
 * 
 * 2007/07/03 - updated to use {@link BitSet}s instead of {@link HashSet}s, as
 * the most expensive operation in this algorithm used to be cloning of the
 * fullSet, which is very cheap for {@link BitSet}s.
 * 
 * @author Navindra Umanee
 * @author Eric Bodden
 **/
public class PostDominator {
	private JCBlock cfg;
	private List<JCTree> heads;
	private BitSet fullSet;
	private Map<JCTree, BitSet> nodeToFlowSet;
	private Map<JCTree, Integer> nodeToIndex;
	private Map<Integer, JCTree> indexToNode;

	private int lastIndex = 0;

	public PostDominator(JCBlock cfg) {
		this.cfg = cfg;
		doAnalysis();
	}

	private final void doAnalysis() {
		heads = cfg.startNodes;
		nodeToFlowSet = new HashMap<JCTree, BitSet>();
		nodeToIndex = new HashMap<JCTree, Integer>();
		indexToNode = new HashMap<Integer, JCTree>();

		// build full set
		fullSet = new BitSet(cfg.stats.size());
		fullSet.flip(0, cfg.stats.size());// set all to true

		// set up domain for intersection: head nodes are only dominated by
		// themselves,
		// other nodes are dominated by everything else
		for (Iterator<JCStatement> i = cfg.stats.iterator(); i.hasNext();) {
			JCStatement o = i.next();
			if (heads.contains(o)) {
				BitSet self = new BitSet();
				self.set(indexOf(o));
				nodeToFlowSet.put(o, self);
			} else {
				nodeToFlowSet.put(o, fullSet);
			}
		}

		boolean changed = true;
		do {
			changed = false;
			for (Iterator<JCStatement> i = cfg.stats.iterator(); i.hasNext();) {
				JCStatement o = i.next();
				if (heads.contains(o))
					continue;

				// initialize to the "neutral element" for the intersection
				// this clone() is fast on BitSets (opposed to on HashSets)
				BitSet predsIntersect = (BitSet) fullSet.clone();

				// intersect over all predecessors
				for (Iterator<JCTree> j = o.getPredecessors().iterator(); j
						.hasNext();) {
					BitSet predSet = nodeToFlowSet.get(j.next());
					predsIntersect.and(predSet);
				}

				BitSet oldSet = nodeToFlowSet.get(o);
				// each node dominates itself
				predsIntersect.set(indexOf(o));
				if (!predsIntersect.equals(oldSet)) {
					nodeToFlowSet.put(o, predsIntersect);
					changed = true;
				}
			}
		} while (changed);
	}

	private int indexOf(JCTree o) {
		Integer index = nodeToIndex.get(o);
		if (index == null) {
			index = lastIndex;
			nodeToIndex.put(o, index);
			indexToNode.put(index, o);
			lastIndex++;
		}
		return index;
	}

	private List<CFGNode> getDominators(Object node) {
		// reconstruct list of dominators from bitset
		List<CFGNode> result = new ArrayList<CFGNode>();
		BitSet bitSet = nodeToFlowSet.get(node);
		for (int i = 0; i < bitSet.length(); i++) {
			if (bitSet.get(i)) {
				result.add(indexToNode.get(i));
			}
		}
		return result;
	}

	public boolean dominates(CFGNode dominator, CFGNode node) {
		return getDominators(node).contains(dominator);
	}

}
