package org.paninij.analysis.staticprofile;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.Tag;

public class BranchPredictionInfo {

	Set<Pair<JCTree, JCTree>> listBackEdges, listExitEdges;
	HashMap<JCTree, Integer> backEdgesCount = new HashMap<JCTree, Integer>();
	Set<JCTree> listCalls, listStores;

	LoopInfo li;

	public BranchPredictionInfo(LoopInfo li) {
		this.li = li;
	}

	private void findBackAndExitEdges() {
		Collection<Loop> loopsVisited = new HashSet<Loop>();
		Collection<JCTree> blocksVisited = new HashSet<JCTree>();
		Iterator<Loop> loopsIter = li.iterator();
		while (loopsIter.hasNext()) {
			Loop root = loopsIter.next();
			JCTree loopHeader = root.getHead();
			// Check if we already visited this loop.
			if (loopsVisited.contains(loopHeader))
				continue;
			// Create a stack to hold loops (inner most on the top).
			Stack<Loop> stack = new Stack<Loop>();
			Collection<JCTree> inStack = new HashSet<JCTree>();

			// Put the current loop into the Stack.
			stack.push(root);
			inStack.add(loopHeader);

			do {
				Loop loop = stack.peek();
				// Search for new inner loops.
				Boolean foundNew = false;
				Iterator<Loop> innerIter = li.inner(loop);
				while (innerIter.hasNext()) {
					Loop inner = innerIter.next();
					JCTree innerHeader = inner.getHead();

					// Skip visited inner loops.
					if (!loopsVisited.contains(inner)) {
						stack.push(inner);
						inStack.add(innerHeader);
						foundNew = true;
						break;
					}
				}

				// If a new loop is found, continue.
				// Otherwise, it is time to expand it, because it is the most
				// inner loop
				// yet unprocessed.
				if (foundNew)
					continue;

				// The variable "loop" is now the unvisited inner most loop.
				JCTree header = loop.getHead();

				// List<Stmt> blocks = loop.getLoopStatements();

				// List<Block> loopBlocks = loop.getLoopBlocks();

				Iterator<JCStatement> blocksIter = loop.loopIterator();

				// Search for all basic blocks on the loop.
				while (blocksIter.hasNext()) {
					JCTree block = blocksIter.next();

					// Ignore the first loop statement, as it is loop head
					// if (block.equals(header))
					// continue;

					// if(blocksVisited.contains(block))
					// continue;

					List<JCTree> successors = block.getSuccessors();

					// For each loop block successor, check if the block
					// pointing is
					// outside the loop.
					for (Iterator<JCTree> succIter = successors.iterator(); succIter
							.hasNext();) {
						JCTree successor = succIter.next();
						Pair<JCTree, JCTree> edge = new Pair<JCTree, JCTree>(
								block, successor);

						// If the successor matches any loop header on the
						// stack, then Back edge
						if (inStack.contains(successor/* .getHead() */))
							listBackEdges.add(edge);

						// If the successor is not present in the loop block
						// list,
						// then it is an exit edge
						// if(loop.jumpsOutOfLoop(block, LI.getGraph()))
						// listExitEdges.add(edge);
						if (!loop.contains(successor))
							listExitEdges.add(edge);
					}
				}

				// Cleaning the visited loop.
				loopsVisited.add(loop);
				stack.pop();
				inStack.remove(header);

			} while (!stack.isEmpty());
		}
	}

	/**
	 * FindCallsAndStores - Search for call and store instruction on basic
	 * blocks.
	 */
	private void findCallsAndStores() {
		Iterator<JCStatement> methodBody = li.methodBody().iterator();
		while (methodBody.hasNext()) {
			JCTree node = methodBody.next();
			if (node instanceof JCMethodInvocation)
				listCalls.add(node);
			else if (node instanceof JCAssign) {
				 JCAssign assignExp = (JCAssign) node;
				 if (assignExp.rhs instanceof JCMethodInvocation)
					 listStores.add(node);
			}
				
		}
	}

	public final Boolean isBackEdge(Pair<JCTree, JCTree> edge) {
		for (Iterator<Pair<JCTree, JCTree>> edgeIter = listBackEdges
				.iterator(); edgeIter.hasNext();) {
			Pair<JCTree, JCTree> edgeLocal = edgeIter.next();
			if (edge.toString().equals(edgeLocal.toString()))
				return true;
		}
		return false;
	}

	public Boolean isExitEdge(Pair<JCTree, JCTree> edge) {
		for (Iterator<Pair<JCTree, JCTree>> edgeIter = listExitEdges
				.iterator(); edgeIter.hasNext();) {
			Pair<JCTree, JCTree> edgeLocal = edgeIter.next();
			if (edge.toString().equals(edgeLocal.toString()))
				return true;
		}
		return false;
	}

	public Boolean hasCall(JCTree basicBlock) {
		// if (listCalls.contains(basicBlock))
		// return true;
		for (Iterator<JCTree> blockIter = listCalls.iterator(); blockIter
				.hasNext();) {
			JCTree listBlock = blockIter.next();
			if (basicBlock.toString().equals(listBlock.toString())) {
				return true;
			}
		}

		return false;
	}

	public Boolean hasStore(JCTree basicBlock) {
		for (Iterator<JCTree> blockIter = listStores.iterator(); blockIter
				.hasNext();) {
			JCTree listBlock = blockIter.next();
			if (basicBlock.toString().equals(listBlock.toString())) {
				return true;
			}
		}
		return false;
	}

	public int CountBackEdges(JCTree block) {
		int count = 0;
		for (Iterator<Pair<JCTree, JCTree>> edgeIter = listBackEdges
				.iterator(); edgeIter.hasNext();) {
			Pair<JCTree, JCTree> edge = edgeIter.next();
			if (edge.first().toString().equals(block.toString())) // ||
																	// edge.second().toString().equals(block.toString())
				count++;
		}
		return count;// backEdgesCount.get(block);
	}

	public boolean CallsExit(JCTree basicBlock) {
		if (hasExitType(basicBlock))	return true;
		return false;
	}

	private boolean hasExitType(JCTree node) {
		if ((node.getTag().equals(Tag.BREAK))
				|| (node.getTag().equals(Tag.CONTINUE))
				|| (node.getTag().equals(Tag.RETURN)))
			return true;
		return false;
	}
}
