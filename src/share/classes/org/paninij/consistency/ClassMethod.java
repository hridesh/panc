package org.paninij.consistency;

import org.paninij.systemgraph.SystemGraph.Node;

import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

// auxiliary class used by the SequentialFIFO
public class ClassMethod {
	public final ClassSymbol cs;
	public final MethodSymbol meth;
	public final Node node;

	public ClassMethod(ClassSymbol cs, MethodSymbol meth, Node node) {
		this.cs = cs;
		this.meth = meth;
		this.node = node;
	}

	public final int hashCode() {
		return cs.hashCode() + meth.hashCode() + node.name.hashCode();
	}

	public final boolean equals(Object obj) {
		if (obj instanceof ClassMethod) {
			ClassMethod other = (ClassMethod)obj;
			return cs.equals(other.cs) &&
			meth.toString().compareTo(other.meth.toString()) == 0 &&
			node.name.equals(other.node.name);
		}
		return false;
	}

	public final String printStr() {
		return node.capsule.name + "." + node.name + "." + cs + "." + meth;
	}
}