package org.paninij.analysis.staticprofile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCForAllLoop;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.JCTree.Tag;

public class Loop {

	private JCStatement header;
	private JCStatement preheader;

	//private List<JCStatement> body = new ArrayList<JCStatement>();

	private JCTree loop;
	private Tag KIND;
	private JCExpression cond;
	private JCBlock body;
	private List<JCStatement> stats;
	
	public Loop(JCDoWhileLoop loop) {
		this.loop = loop;
		this.KIND = loop.getTag();
		this.body = (JCBlock) loop.body;
		this.stats = this.body.stats;
	}
	
	public Loop(JCForAllLoop loop) {
		this.loop = loop;
		this.KIND = loop.getTag();
		this.body = (JCBlock) loop.body;
		this.stats = this.body.stats;
	}
	
	public Loop(JCForLoop loop) {
		this.loop = loop;
		this.KIND = loop.getTag();
		this.body = (JCBlock) loop.body;
		this.stats = this.body.stats;
	}
	
	public Loop(JCWhileLoop loop) {
		this.loop = loop;
		this.KIND = loop.getTag();
		this.body = (JCBlock) loop.body;
		this.stats = this.body.stats;
	}

	public JCTree getHead() {
		return header;
	}

	public JCTree getPreHeader() {
		return preheader;
	}
	
	public Iterator<JCStatement> loopIterator() {
		return this.stats.iterator();
	}
	
	public List<JCStatement> nodes () {
		return this.stats;
	}
	
	public Iterator<JCStatement> iterator () {
		return this.stats.iterator();
	}
	
	public boolean contains(JCTree node) {
		if (this.stats.contains(node))	return true;
		return false;
	}
}
