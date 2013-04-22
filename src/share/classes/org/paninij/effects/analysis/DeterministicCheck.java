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

package org.paninij.effects.analysis;

import static com.sun.tools.javac.code.TypeTags.ARRAY;

import org.paninij.systemgraph.*;
import org.paninij.systemgraph.SystemGraph.*;
import java.util.*;

import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol.CapsuleSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type.ArrayType;

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
		HashSet<ClassMethoNode> traversed = new HashSet<ClassMethoNode>();
		for (Node node : graph.nodes.values()) {
			CapsuleSymbol cs = node.capsule;
			for (MethodSymbol ms : node.procedures) {
				if (cs.definedRun && ms.toString().compareTo("run()") == 0) {
					ClassMethoNode now = new ClassMethoNode(cs, ms, node);
					if (traversed.contains(now)) {
						continue;
					}
					traversed.add(now);
					paths.clear();
					ArrayList<ClassMethoNode> al =
						new ArrayList<ClassMethoNode>();
					// al.add(new ClassMethoNode(node.capsule, ms));
					traverse(node, ms, al);
					checkPaths(paths);
				}
			}
		}
	}

	private final void checkPaths(HashSet<ArrayList<ClassMethoNode>> paths) {
		int i = 0;
		for (ArrayList<ClassMethoNode> path1 : paths) {
			int j = 0;
			for (ArrayList<ClassMethoNode> path2 : paths) {
				if (i < j) {
					if (path1 != path2) {
						ArrayList<ClassMethoNode[]> pairs = getPairs(path1, path2);
						for (ClassMethoNode[] pair : pairs) {
							ClassMethoNode cmn1 = pair[0];
							ClassMethoNode cmn2 = pair[1];
							EffectSet effects1 = null;
							EffectSet effects2 = null;
	
							for (MethodSymbol ms : cmn1.node.procedures) {
								effects1 = matchMeth(ms, cmn1.meth);
								if (effects1 != null) {
									break;
								}
							}
	
							if (effects1 == null || effects1.isBottom) {
								for (MethodSymbol ms : cmn1.node.procedures) {
									if (ms.toString().compareTo(
											cmn1.meth.toString()) == 0) {
										if (ms.ars != null && !ms.ars.isBottom) {
											effects1 = ms.ars;
										}
									}
								}
							}
	
							for (MethodSymbol ms : cmn2.node.procedures) {
								effects2 = matchMeth(ms, cmn2.meth);
								if (effects2 != null) {
									break;
								}
							}
	
							if (effects2 == null || effects2.isBottom) {
								for (MethodSymbol ms : cmn2.node.procedures) {
									if (ms.toString().compareTo(
											cmn2.meth.toString()) == 0) {
										if (ms.ars != null && !ms.ars.isBottom) {
											effects2 = ms.ars;
										}
									}
								}
							}
	
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

	private static final EffectSet matchMeth(MethodSymbol ms,
			MethodSymbol ms1) {
		if (ms.toString().indexOf("$Original") != -1) {
			int start = ms.toString().indexOf("$Original");
			int end = start + 9;
			String temp = ms.toString().substring(0, start) +
			ms.toString().substring(end);
			if (temp.compareTo(ms1.toString()) == 0) {
				return ms.ars;
			}

			String temp1 = types(ms1);
			if (temp.compareTo(temp1) == 0) {
				return ms.ars;
			}
		}
		return null;
	}

	public static final String types(MethodSymbol ms) {
		List<Type> args = ms.type.getParameterTypes();
		StringBuilder buf = new StringBuilder();
		buf.append(ms.name.toString() + "(");

		if (!args.isEmpty()) {
			while (args.tail.nonEmpty()) {
				String temp = args.head.toString();
				int index = temp.indexOf("<");

				// remove the polymorphic type info
				if (index == -1) { buf.append(temp);
				} else { buf.append(temp.substring(0, index)); }
				args = args.tail;
				buf.append(',');
			}
			if (args.head.tag == ARRAY) {
				buf.append(((ArrayType)args.head).elemtype);
				buf.append("...");
			} else {
				String temp = args.head.toString();
				int index = temp.indexOf("<");
	
				// remove the polymorphic type info
				if (index == -1) { buf.append(temp);
				} else { buf.append(temp.substring(0, index)); }
			}
		}
		
		buf.append(")");
		return buf.toString();
    }

	private final ArrayList<ClassMethoNode[]> getPairs(
			ArrayList<ClassMethoNode> path1, ArrayList<ClassMethoNode> path2) {
		ArrayList<ClassMethoNode[]> result = new ArrayList<ClassMethoNode[]>();
		int i = 0;
		for (ClassMethoNode cmn1 : path1) {
			if (i > 0) {
				int j = 0;
				for (ClassMethoNode cmn2 : path2) {
					if (j > 0) {
						if (cmn1.node == cmn2.node) {
							// FIFO of same reveiver and sender
							if (i != 1 || j != 1) {
								result.add(new ClassMethoNode[]{cmn1, cmn2});
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
			HashSet<EffectEntry> s2, ArrayList<ClassMethoNode> path1,
			ArrayList<ClassMethoNode> path2) {
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

	private final String printPath (ArrayList<ClassMethoNode> path) {
		String s = new String();
		for (ClassMethoNode cmn : path) {
			s += cmn.cs + "." + cmn.node.name.toString() + "." +
			cmn.meth + "->";
		}
		return s;
	}

	private final HashSet<ArrayList<ClassMethoNode>> paths =
		new HashSet<ArrayList<ClassMethoNode>>();

	private final void traverse(Node node, MethodSymbol ms,
			ArrayList<ClassMethoNode> curr) {
		ClassMethoNode temp = new ClassMethoNode(node.capsule, ms, node);
		ArrayList<ClassMethoNode> newList = new ArrayList<ClassMethoNode>(curr);
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

	public final class ClassMethoNode extends Object {
		public final CapsuleSymbol cs;
		public final MethodSymbol meth;
		public final Node node;

		public ClassMethoNode(CapsuleSymbol cs, MethodSymbol meth, Node node) {
			this.cs = cs;
			this.meth = meth;
			this.node = node;
		}

		public final int hashCode() {
			return cs.hashCode() + meth.hashCode() + node.name.hashCode();
		}

		public final boolean equals(Object obj) {
	        if (obj instanceof ClassMethoNode) {
	        	ClassMethoNode other = (ClassMethoNode)obj;
	        	return cs.equals(other.cs) &&
	        	meth.toString().compareTo(other.meth.toString()) == 0 &&
	        	node.name.equals(other.node.name);
	        }
	        return false;
	    }
	}
}