package org.paninij.effects;

import java.util.ArrayList;
import com.sun.tools.javac.tree.*;

public class ASTChainNode {
	public JCTree tree;
	public ArrayList<ASTChainNode> next = new ArrayList<ASTChainNode>();
	public ArrayList<ASTChainNode> previous = new ArrayList<ASTChainNode>();
    public ArrayList<ASTChainNode> startNodes = new ArrayList<ASTChainNode>();
    public ArrayList<ASTChainNode> endNodes = new ArrayList<ASTChainNode>();
    public ArrayList<ASTChainNode> excEndNodes = new ArrayList<ASTChainNode>();
    boolean startNodesTainted = true;
    boolean endNodesTainted = true;

	public ASTChainNode(JCTree tree) {
		this.tree = tree;
	}

	public int hashCode() {
		return tree.hashCode(); 
	}

	public boolean equals(Object o) {
		if (o instanceof ASTChainNode){
			ASTChainNode g = (ASTChainNode)o;
			return tree.equals(g.tree);
		}
		return false;
	}
}