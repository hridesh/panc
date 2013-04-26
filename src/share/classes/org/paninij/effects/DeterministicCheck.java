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
 * Contributor(s): Eric Lin, Yuheng Long
 */

package org.paninij.effects;

import org.paninij.systemgraph.*;
import org.paninij.systemgraph.SystemGraph.*;
import java.util.*;

import com.sun.tools.javac.util.*;
import com.sun.tools.javac.code.Symbol.CapsuleSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

public class DeterministicCheck {
	SystemGraph graph;
	Log log;
	public DeterministicCheck(SystemGraph graph, Log log) {
		this.graph = graph;
		this.log = log;
	}

	/**Do the first part of the analysis.
	 * this checks if there are more than one simple paths between any two
	 * vertices of the graph.
	 */
	public void potentialPathCheck() {
		HashSet<ClassMethodNode> traversed = new HashSet<ClassMethodNode>();
		for (Node node : graph.nodes.values()) {
			CapsuleSymbol cs = node.capsule;
			for (MethodSymbol ms : node.procedures) {
				if (cs.definedRun && ms.toString().compareTo("run()") == 0) {
					ClassMethodNode now = new ClassMethodNode(cs, ms, node);
					if (traversed.contains(now)) {
						continue;
					}
					traversed.add(now);
					paths.clear();
					ArrayList<ClassMethodNode> al =
						new ArrayList<ClassMethodNode>();
					// al.add(new ClassMethodNode(node.capsule, ms));
					traverse(node, ms, al);
System.out.println("potentialPathCheck " + node.capsule);
for (ArrayList<ClassMethodNode> path : paths) {
	System.out.println("\t" + printPath(path));
}
					checkPaths(paths);
				}
			}
		}
	}

	private final void checkPaths(HashSet<ArrayList<ClassMethodNode>> paths) {
		int i = 0;
		for (ArrayList<ClassMethodNode> path1 : paths) {
			int j = 0;
			for (ArrayList<ClassMethodNode> path2 : paths) {
				if (i < j) {
					if (path1 != path2) {
						ArrayList<ClassMethodNode[]> pairs = getPairs(path1, path2);
						for (ClassMethodNode[] pair : pairs) {
							ClassMethodNode cmn1 = pair[0];
							ClassMethodNode cmn2 = pair[1];
							EffectSet effects1 = cmn1.meth.effect;
							EffectSet effects2 = cmn2.meth.effect;
	
							if (effects1 != null && effects2 != null) {
								if (effects1.isBottom) {
									log.warning(
											"deterministic.inconsistency.warning",
											printPath(path1), printPath(path2));
								}
								if (effects2.isBottom) {
									log.warning(
											"deterministic.inconsistency.warning",
											printPath(path1), printPath(path2));
								}
								detectEntries(effects1.write, effects2.write, path1, path2);
								detectEntries(effects1.write, effects2.read, path1, path2);
								detectEntries(effects1.read, effects2.write, path1, path2);
							}
						}
					}
				}
				j++;
			}
			i++;
		}
	}

	private final ArrayList<ClassMethodNode[]> getPairs(
			ArrayList<ClassMethodNode> path1, ArrayList<ClassMethodNode> path2) {
		ArrayList<ClassMethodNode[]> result = new ArrayList<ClassMethodNode[]>();
		int i = 0;
		for (ClassMethodNode cmn1 : path1) {
			if (i > 0) {
				int j = 0;
				for (ClassMethodNode cmn2 : path2) {
					if (j > 0) {
						if (cmn1.node == cmn2.node) {
							// FIFO of same reveiver and sender
							if (i != 1 || j != 1) {
								result.add(new ClassMethodNode[]{cmn1, cmn2});
							}
						}
					}
					j++;
				}
			}
			i++;
		}
		return result;
	}

	private final void detectEntries(HashSet<EffectEntry> s1,
			HashSet<EffectEntry> s2, ArrayList<ClassMethodNode> path1,
			ArrayList<ClassMethodNode> path2) {
		for (EffectEntry ee1 : s1) {
			for (EffectEntry ee2 : s2) {
				if (ee1 instanceof ArrayEffect) {
					if (ee2 instanceof ArrayEffect) {
						ArrayEffect ae1 = (ArrayEffect)ee1;
						ArrayEffect ae2 = (ArrayEffect)ee2;
						if (ae1.path.equals(ae2)) {
							log.warning("deterministic.inconsistency.warning",
									printPath(path1), printPath(path2));
						}
					}
				}
				if (ee1 instanceof FieldEffect) {
					if (ee2 instanceof FieldEffect) {
						FieldEffect ae1 = (FieldEffect)ee1;
						FieldEffect ae2 = (FieldEffect)ee2;

						if (ae1.f.equals(ae2.f)) {
							log.warning("deterministic.inconsistency.warning",
									printPath(path1), printPath(path2));
						}
					}
				}
			}
		}
	}

	private final String printPath (ArrayList<ClassMethodNode> path) {
		String s = new String();
		for (ClassMethodNode cmn : path) {
			s += cmn.cs + "." + cmn.node.name.toString() + "." +
			cmn.meth + "->";
		}
		return s;
	}

	private final HashSet<ArrayList<ClassMethodNode>> paths =
		new HashSet<ArrayList<ClassMethodNode>>();

	private final void traverse(Node node, MethodSymbol ms,
			ArrayList<ClassMethodNode> curr) {
		ClassMethodNode temp = new ClassMethodNode(node.capsule, ms, node);
		ArrayList<ClassMethodNode> newList = new ArrayList<ClassMethodNode>(curr);
		if (curr.contains(temp)) {
			paths.add(newList);
			return;
		}

		newList.add(temp);
		int numEdge = 0;
		for (Edge e : graph.edges) {
			if (e.fromNode == node &&
					e.fromProcedure.toString().compareTo(ms.toString()) == 0) {
				traverse(e.toNode, e.toProcedure, newList);
				numEdge++;
			}
		}
		if (numEdge == 0) {
			paths.add(newList);
		}
	}

	public final class ClassMethodNode extends Object {
		public final CapsuleSymbol cs;
		public final MethodSymbol meth;
		public final Node node;

		public ClassMethodNode(CapsuleSymbol cs, MethodSymbol meth, Node node) {
			this.cs = cs;
			this.meth = meth;
			this.node = node;
		}

		public final int hashCode() {
			return cs.hashCode() + meth.hashCode() + node.name.hashCode();
		}

		public final boolean equals(Object obj) {
	        if (obj instanceof ClassMethodNode) {
	        	ClassMethodNode other = (ClassMethodNode)obj;
	        	return cs.equals(other.cs) &&
	        	meth.toString().compareTo(other.meth.toString()) == 0 &&
	        	node.name.equals(other.node.name);
	        }
	        return false;
	    }
	}
}