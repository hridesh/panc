/*
 * This file is part of the Panini project at Iowa State University.
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 * 
 * For more details and the latest version of this code please see
 * http://paninij.org
 * 
 * Contributor(s): Yuheng Long
 */

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
