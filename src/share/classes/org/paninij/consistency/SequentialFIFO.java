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
 * Contributor(s): Yuheng Long, Sean L. Mooney
 */

package org.paninij.consistency;

import org.paninij.systemgraph.*;
import org.paninij.systemgraph.SystemGraph.*;

import java.util.*;

import com.sun.tools.javac.util.*;
import com.sun.tools.javac.code.Symbol.*;

import org.paninij.effects.*;

// This version of the sequential consistency violation detector considers the
// FIFO of the Capsule message queue.
public class SequentialFIFO implements SeqConstCheckAlgorithm {
	private SystemGraph graph;
	private Log log;
	private final boolean debug = false;

	public SequentialFIFO(SystemGraph graph, Log log) {
		this.graph = graph;
		this.log = log;
	}

	// all the loops for the capsule methods.
	private final HashMap<ClassMethod, HashSet<Route>> loops =
		new HashMap<ClassMethod, HashSet<Route>>();

	private final HashSet<Route> paths = new HashSet<Route>();

	private static final class BiRoute {
		final Route r1;
		final Route r2;

		public BiRoute(Route r1, Route r2) {
			this.r1 = r1;
			this.r2 = r2;
		}

		public final int hashCode() {
			return r1.hashCode() + r2.hashCode();
		}

		public final boolean equals(Object obj) {
	        if (obj instanceof BiRoute) {
	        	BiRoute other = (BiRoute)obj;
	        	return r1.equals(other.r1) && r2.equals(other.r2);
	        }
	        return false;
	    }
	}

	public HashSet<BiRoute> warnings = new HashSet<BiRoute>();

	@Override
	public void potentialPathCheck() {
if (debug) {
System.out.println("edge");
for (Edge ee : graph.edges) {
	System.out.println(ee.fromNode.name + "." + ee.fromProcedure + " -"
			+ ee.line + "-> " + ee.toNode.name + "."+ ee.toProcedure);
}
}

		HashSet<ClassMethod> traversed = new HashSet<ClassMethod>();
		for (Node node : graph.nodes.values()) {
			CapsuleSymbol cs = node.capsule;
			for (MethodSymbol ms : node.procedures) {
				if (cs.definedRun && ms.toString().compareTo("run()") == 0) {
					ClassMethod now = new ClassMethod(cs, ms, node);

					if (traversed.contains(now)) { continue; }
					traversed.add(now);

					paths.clear();
					Route al = new Route();
					// al.add(new ClassMethodNode(node.capsule, ms));
					traverse(node, null, ms, al);
// if (debug) {
System.out.println("potentialPathCheck " + node.capsule);
for (Route path : paths) {
	System.out.println("\t" + path.routeStr());
}
// }
					checkPaths(paths);
				}
			}
		}

// if (debug) {
int i = 0;
for (BiRoute br : warnings) {
	System.out.println("warning " + (i++));
	System.out.println("\t" + br.r1.routeStr());
	System.out.println("\t" + br.r2.routeStr());
}
// }
System.out.println("SF warnings = " + warnings.size());
System.out.println("loops = " + loops.size());
	}

	private final void checkPaths(HashSet<Route> paths) {
		int i = 0;
		for (Route path1 : paths) {
			int j = 0;
			for (Route path2 : paths) {
				// if (i != j) {
if (debug) {
	System.out.println("checkPaths0 i = " + i + "\tj = " + j);
path1.printRoute();
path2.printRoute();
}

					ArrayList<ClassMethod[]> pairs = getPairs(path1, path2);
					for (ClassMethod[] pair : pairs) {
						ClassMethod cmn1 = pair[0];
						ClassMethod cmn2 = pair[1];
						EffectSet es1 = cmn1.meth.effect;
						EffectSet es2 = cmn2.meth.effect;

						if (es1 != null && es2 != null) {
							Route t1 = path1.clonePrefixPath(cmn1);
							Route t2 = path2.clonePrefixPath(cmn2);
							if (es1.isBottom || es2.isBottom) {
								pathsAlgorithm(t1, t2, path1, path2);
							}
							detect(es1.write, es2.write, t1, t2, path1, path2);
							detect(es1.write, es2.read, t1, t2, path1, path2);
							detect(es1.read, es2.write, t1, t2, path1, path2);
						}
					}
				// }
				j++;
			}
			i++;
		}
	}

	private final void pathsAlgorithm(Route r1, Route r2, Route er1, Route er2) {
if (debug) {
System.out.println("pathsAlgorithm0");
r1.printRoute();
r2.printRoute();
}
		int size1 = r1.size();
		int size2 = r2.size();
		ArrayList<ClassMethod> n1 = r1.nodes;
		ArrayList<ClassMethod> n2 = r2.nodes;
		ArrayList<Edge> e1 = r1.edges;
		ArrayList<Edge> e2 = r2.edges;

		int i = 0;

		while (i < size1 - 1 && i < size2 - 1 && n1.get(i).equals(n2.get(i))
				&& e1.get(i).equals(e2.get(i))) {
			Edge ee = e1.get(i);
			EffectSet es = n1.get(i).meth.effect;
			HashSet<BiCall> pair = new HashSet<BiCall>(es.direct);
			pair.addAll(es.indirect);
			ClassMethod cm = n1.get(i);

			for (BiCall bc : pair) {
				CallEffect ce1 = bc.ce1;
				CallEffect ce2 = bc.ce2;
				if (ce1.equals(ce2)) {
					if (ee.pos == ce1.pos()) {
						distinctPath(r1.cloneSubPath(cm), r2.cloneSubPath(cm),
								er1, er2);
					}
				}
			}
			i++;
		}
if (debug) {
System.out.println("pathsAlgorithm1 i = " + i);
}
		if (i < size1 - 1 && i < size2 - 1) {
if (debug) {
System.out.println("pathsAlgorithm2 i = " + i);
}
			ClassMethod cm = n1.get(i);
			distinctPath(r1.cloneSubPath(cm), r2.cloneSubPath(cm), er1, er2);
		}
	}

	// This method should be called when the first nodes of the two routes are
	// the same
	private final void distinctPath(Route r1, Route r2, Route er1, Route er2) {
if (debug) {
System.out.println("distinctPath0");
r1.printRoute();
r2.printRoute();
}
		ArrayList<ClassMethod> ns1 = r1.nodes;
		ArrayList<Edge> l1 = r1.edges;
		ArrayList<ClassMethod> ns2 = r2.nodes;
		ArrayList<Edge> l2 = r2.edges;

		ClassMethod h1 = ns1.get(0);
		ClassMethod h2 = ns2.get(0);

		// the first node should be the same
		if (!h1.equals(h2)) { throw new Error(); }

		// TODO(yuhenglong): what about foreach
		MethodSymbol ms = h1.meth;
		EffectSet es = ms.effect;
		Edge e1 = l1.get(0);
		Edge e2 = l2.get(0);
		int pos1 = e1.pos;
		int pos2 = e2.pos;
		int size2 = ns2.size();
if (debug) {
System.out.println("distinctPath1");
}
		int j = 0;
		HashSet<Route> paths = loops.get(h1);
		if (paths != null) {
			boolean changed;
			do {
				changed = false;
				for (Route r : paths) {
					int temp = j;
					j = check(r, 0, r2, j, er1, er2);

					if (j >= size2 - 1) { return; }
					if (j != temp) {
						changed = true;
					}
				}
			} while(changed);
		}

		if (j >= size2 - 1) {
			/*log.warning("deterministic.inconsistency.warning",
					er1.routeStr(), er2.routeStr());*/
			warnings.add(new BiRoute(er1, er2));
			return;
		}

		int size1 = ns1.size();

		if (j != 0) {
			int i = 0;
			ClassMethod cm = ns1.get(0);
			Edge ee = l1.get(0);

			while (i < size1 - 1 && synchronousCall(cm, ee.pos)) {
				i++;
				cm = ns1.get(i);
				ee = l1.get(i);
			}
			if (i < size1 - 1) { check(r1, i, r2, j, er1, er2); }
			return;
		}

		HashSet<BiCall> direct = es.direct;
		boolean existReverse = false;
		for (BiCall bc : direct) {
			CallEffect ce1 = bc.ce1;
			CallEffect ce2 = bc.ce2;

			// match
			if (ce1.pos() == pos1 && ce2.pos() == pos2) {
if (debug) {
System.out.println("distinctPath1.1");
}
				check(r1, 1, r2, 1, er1, er2);
				return;
			} else if (ce1.pos() == pos2 && ce2.pos() == pos1) {
if (debug) {
System.out.println("distinctPath1.2");
}
				// return;
				existReverse = true;
			}
		}
		if (existReverse) { return; }
if (debug) {
System.out.println("distinctPath2");
}
		HashSet<BiCall> indirect = es.indirect;
		for (BiCall bc : indirect) {
			CallEffect ce1 = bc.ce1;
			CallEffect ce2 = bc.ce2;

			// match
			if (ce1.pos() == pos1 && ce2.pos() == pos2) {
				if (2 < size1) {
					int i = 1;
					ClassMethod cm = ns1.get(i);
					Edge ee = l1.get(i);
					while (i < size1 - 1 && synchronousCall(cm, ee.pos)) {
						i++;
						cm = ns1.get(i);
						ee = l1.get(i);
					}
					if (i < size1 - 1) { check(r1, i + 1, r2, j, er1, er2); }
				}
				return;
			} /*else if (ce1.pos() == pos2 && ce2.pos() == pos1) {
				return;
			}*/
		}
if (debug) {
System.out.println("distinctPath3");
}
	}

	private final boolean synchronousCall(ClassMethod cm, int pos) {
		HashSet<CallEffect> collected = cm.meth.effect.collected;
		for (CallEffect ce : collected) {
			if (pos == ce.pos()) { return true; }
		}
		return false;
	}

	// this method should be called when the first edge of the first path is
	// asychronous call.
	private final int check(Route r1, int i, Route r2, int j, Route er1,
			Route er2) {
if (debug) {
System.out.println("check0 i = " + i + "\tj = " + j);
r1.printRoute();
r2.printRoute();
}
		int size1 = r1.size();
		int size2 = r2.size();
		ArrayList<ClassMethod> ns1 = r1.nodes;
		ArrayList<Edge> l1 = r1.edges;
		ArrayList<ClassMethod> ns2 = r2.nodes;
if (debug) {
System.out.println("check0.5 s1 = " + size1 + "\ts2 = " + size2);
}
		for (; i < size1 - 1;) {
if (debug) {
System.out.println("\tcheck1 i = " + i + "\tj = " + j);
}
			ClassMethod cm = ns1.get(i);
			for (; j < size2 - 1; j++) {
if (debug) {
System.out.println("\t\tcheck2 i = " + i + "\tj = " + j);
System.out.println("\t\t\tcm = " + cm.printStr() + "\tcm2 = " + ns2.get(j).printStr());
}
				ClassMethod cm2 = ns2.get(j);
				if (cm.node.equals(cm2.node)) {
					i++;
					break;
				}
				ClassMethod cm2p1 = ns2.get(j + 1);
				if (cm.node.equals(cm2p1.node)) {
					i++;
					j++;
					break;
				}
			}
			if (j >= size2 - 1 && i < size1 - 1) {
if (debug) {
System.out.println("\tcheck3 i = " + i + "\tj = " + j);
}
				/*log.warning("deterministic.inconsistency.warning",
						er1.routeStr(), er2.routeStr());*/
				warnings.add(new BiRoute(er1, er2));
				return size2 - 1;
			}

			if (i < size1 - 1) {
				cm = ns1.get(i);
				HashSet<Route> paths = loops.get(cm);
				if (paths != null) {
					boolean changed;
					do {
						changed = false;
						for (Route r : paths) {
							int temp = j;
							j = check(r, 0, r2, j, er1, er2);
							if (j != temp) {
								changed = true;
							}
						}
					} while(changed);
				}
if (debug) {
System.out.println("\tcheck4 i = " + i + "\tj = " + j);
}
				Edge ee = l1.get(i - 1);
				ClassMethod cmm1 = ns1.get(i - 1);
				while (i < size1 - 1 && synchronousCall(cmm1, ee.pos)) {
					i++;
if (debug) {
System.out.println("\t\tcheck5 i = " + i + "\tj = " + j);
}
					if (i < size1 - 1) {
						cmm1 = ns1.get(i - 1);
						ee = l1.get(i - 1);
					}
				}
			}
		}
		return j;
	}

	private final ArrayList<ClassMethod[]> getPairs(Route r1, Route r2) {
		ArrayList<ClassMethod> path1 = r1.nodes;
		ArrayList<ClassMethod> path2 = r2.nodes;
		ArrayList<ClassMethod[]> result = new ArrayList<ClassMethod[]>();
		int i = 0;
		for (ClassMethod cmn1 : path1) {
			if (i > 0) {
				int j = 0;
				for (ClassMethod cmn2 : path2) {
					if (j > 0) {
						if (cmn1.node == cmn2.node) {
							// FIFO of same reveiver and sender
							// if (i != 1 || j != 1) {
								result.add(new ClassMethod[]{cmn1, cmn2});
							// }
						}
					}
					j++;
				}
			}
			i++;
		}
		return result;
	}

	private final void detect(HashSet<EffectEntry> s1, HashSet<EffectEntry> s2,
			Route path1, Route path2, Route er1, Route er2) {
		for (EffectEntry ee1 : s1) {
			for (EffectEntry ee2 : s2) {
				if (ee1 instanceof ArrayEffect) {
					if (ee2 instanceof ArrayEffect) {
						ArrayEffect ae1 = (ArrayEffect)ee1;
						ArrayEffect ae2 = (ArrayEffect)ee2;
						if (ae1.path.equals(ae2)) {
							pathsAlgorithm(path1, path2, er1, er2);
						}
					}
				}
				if (ee1 instanceof FieldEffect) {
					if (ee2 instanceof FieldEffect) {
						FieldEffect ae1 = (FieldEffect)ee1;
						FieldEffect ae2 = (FieldEffect)ee2;

						if (ae1.f.equals(ae2.f)) {
							pathsAlgorithm(path1, path2, er1, er2);
						}
					}
				}
			}
		}
	}

	private final void traverse(Node node, Edge e, MethodSymbol ms, Route curr) {
		ClassMethod temp = new ClassMethod(node.capsule, ms, node);
		Route newList = curr.cloneR();
		if (curr.nodes.contains(temp)) {
			// add the currently detected loop
			Route loop = curr.cloneSubPath(temp);

			HashSet<Route> hs = loops.get(temp);
			if (hs == null) {
				hs = new HashSet<Route>();
				loops.put(temp, hs);
			}
			hs.add(loop);

			paths.add(newList);
			return;
		}

		if (e != null) {
			newList.edges.add(e);
		}
		newList.nodes.add(temp);

		int numEdge = 0;
		for (Edge ee : graph.edges) {
			if (ee.fromNode == node &&
					ee.fromProcedure.toString().compareTo(ms.toString()) == 0) {
				traverse(ee.toNode, ee, ee.toProcedure, newList);
				numEdge++;
			}
		}
		if (numEdge == 0) { paths.add(newList); }
	}
}
