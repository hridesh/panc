package org.paninij.analysis.staticprofile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCapsuleDecl;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCForAllLoop;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.JCTree.Tag;

public class LoopInfo {

	private JCCapsuleDecl module;
	private JCMethodDecl method;
	private Collection<Loop> loops;

	public LoopInfo(JCCapsuleDecl module, JCMethodDecl m) {
		this.module = module;
		this.method = m;
		this.loops = new HashSet<Loop>();
		loopfinder();
	}

	private void loopfinder() {
		JCBlock body = this.method.body;
		if (body == null)	return;
		List<JCStatement> methodBody = body.getStatements();
		Iterator<JCStatement> statsIter = methodBody.iterator();
		while (statsIter.hasNext()) {
			JCTree node = statsIter.next();
			Loop loop = null;
			if ((loop = isLoop(node)) != null) {
				loops.add(loop);
			}
		}
	}

	// Currently not used.. but keeping it
	private Loop isLoop(JCTree tree) {
		//JCTree tree = node.tree;
//		if ((tree.getTag() == Tag.DOLOOP) || (tree.getTag() == Tag.WHILELOOP)
//				|| (tree.getTag() == Tag.FORLOOP)
//				|| (tree.getTag() == Tag.FORALLLOOP)
//				|| (tree.getTag() == Tag.FOREACHLOOP)) {
//			return true;
//		}
//		return false;
		Loop loop = null;
		if (tree.getTag() == Tag.DOLOOP) {
			loop = new Loop ((JCDoWhileLoop) tree);
		} else if (tree.getTag() == Tag.WHILELOOP) {
			loop = new Loop ((JCWhileLoop) tree);
		} else if (tree.getTag() == Tag.FORLOOP) {
			loop = new Loop ((JCForLoop) tree);
		} else if (tree.getTag() == Tag.FORALLLOOP) {
			loop = new Loop ((JCForAllLoop) tree);
		} 
		return loop;
	}
	
	public Iterator<Loop> iterator() {
		return loops.iterator();
	}
	
	//TODO: do this while parsing CFGLoopNode itself
	private Loop nestedLoop (JCTree node) {
		Iterator<Loop> loopsIter = loops.iterator();
		while (loopsIter.hasNext()) {
			Loop l = loopsIter.next();
			if (l.getHead().equals(node))	return l;
		}
		return null;
	}
	
	//TODO: do this while parsing CFGLoopNode itself
	public Iterator<Loop> inner(Loop root) {
		Collection<Loop> inner = new HashSet<Loop>();
		Iterator<JCStatement> nodesIter = root.iterator();
		while (nodesIter.hasNext()) {
			JCStatement node = nodesIter.next();
			if (node.equals(root.getHead()))	continue;
			Loop l;
			if ((l = nestedLoop(node)) != null)
				inner.add(l);
		}
		return inner.iterator();
	}
	
	public List<JCStatement> methodBody () {
		if (method.body == null)	return new ArrayList<JCTree.JCStatement>();
		return this.method.body.getStatements();
	}
	
	public JCTree first () {
		if (this.method.body == null)	return null;
		return this.method.body.getStatements().head;
	}
	
	//TODO: do this while parsing CFGLoopNode itself
	public Loop returnContainingLoop(JCTree node) {
		for (Loop loop : loops) {
			if (loop.contains(node))
				return loop;
		}
		return null;
	}
	
	//TODO: do this while parsing CFGLoopNode itself
	public boolean isLoopHeader(JCTree node) {
		return false;
	}
	
	//TODO: do this while parsing CFGLoopNode itself
	public boolean loopPreHeader(JCTree node) {
		return false;
	}
}
