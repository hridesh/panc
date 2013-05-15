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

// This version of the sequential consistency violation detector signals warning
// when two paths conflict.
public class V1 implements SeqConstCheckAlgorithm {
	private SystemGraph graph;
	private Log log;

	private final HashSet<Route> paths = new HashSet<Route>();

	public V1(SystemGraph graph, Log log) {
		this.graph = graph;
		this.log = log;
	}

	// all the loops for the capsule methods.
	private final HashMap<ClassMethod, HashSet<Route>> loops =
		new HashMap<ClassMethod, HashSet<Route>>();

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

	public void potentialPathCheck() {
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
					traverse(node, null, ms, al);

					checkPaths(paths);
				}
			}
		}

        System.out.println("V1 warnings = " + warnings.size());
        System.out.println("V1 trim warnings = " + trim(warnings).size());
	}

	// trim the warnings.
    private static final HashSet<BiRoute>trim(HashSet<BiRoute> warnings) {
        HashSet<BiRoute> result = new HashSet<BiRoute>();
        for (BiRoute br : warnings) {
            Route rt1 = new Route();
            Route rt2 = new Route();
            Route r1 = br.r1;
            Route r2 = br.r2;

            int s1 = r1.size();
            int s2 = r2.size();
            for (int i = 0; i < s1 && i < s2; i++) {
                ClassMethod cm1 = r1.nodes.get(i);
                ClassMethod cm2 = r2.nodes.get(i);

                if (cm1.equals(cm2)) {
                    rt1.nodes.add(cm1);
                    rt2.nodes.add(cm2);
                } else {
                    break;
                }

                if (i != s1 - 1 && i != s2 - 1) {
                    Edge e1 = r1.edges.get(i);
                    Edge e2 = r2.edges.get(i);
                    rt1.edges.add(e1);
                    rt2.edges.add(e2);
                    if (!e1.equals(e2)) {
                        break;
                    }
                }
            }

            BiRoute temp = new BiRoute(rt1, rt2);
            int tes1 = rt1.edges.size();
            int tes2 = rt2.edges.size();
            int ns1 = rt1.nodes.size();
            int ns2 = rt2.nodes.size();

            boolean found = false;
            for (BiRoute curr : result) {
                Route c1 = curr.r1;
                Route c2 = curr.r2;
                int sc1 = c1.size();
                int sc2 = c2.size();

                int es1 = c1.edges.size();
                int es2 = c2.edges.size();
                if (es1 == tes1 && es2 == tes2 && ns1 == sc1 && ns2 == sc2) {
                    int i = 0;
                    for (i = 0; i < ns1 && i < ns2; i++) {
                        ClassMethod cm1 = rt1.nodes.get(i);
                        ClassMethod cm2 = rt2.nodes.get(i);

                        ClassMethod cm3 = c1.nodes.get(i);
                        ClassMethod cm4 = c2.nodes.get(i);

                        if (!isomorphicNodes(cm1, cm3) ||
                              !isomorphicNodes(cm2, cm4)) {
                            break;
                        }

                        if (i < es1 && i < es2) {
                            Edge e1 = r1.edges.get(i);
                            Edge e2 = r2.edges.get(i);

                            Edge e3 = c1.edges.get(i);
                            Edge e4 = c2.edges.get(i);
                            if (e1.pos != e3.pos || e2.pos != e4.pos) {
                                break;
                            }
                        }
                    }
                    if (i == ns1 || i == ns2) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                result.add(temp);
            }
        }
        return result;
    }

    private static final boolean isomorphicNodes(ClassMethod cm1,
        ClassMethod cm2) {
        if (cm1.equals(cm2)) {
            return true;
        }
        if (cm1.cs.equals(cm2.cs) && cm1.meth.toString().compareTo(
            cm2.meth.toString()) == 0) {
            Node n1 = cm1.node;
            Node n2 = cm2.node;
            String s1 = n1.name.toString();
            String s2 = n2.name.toString();
            int f1 = s1.indexOf("[");
            int f2 = s2.indexOf("[");

            int e1 = s1.indexOf("]");
            int e2 = s2.indexOf("]");

            if (f1 != -1 && f2 != -1) {
                if (s1.substring(0, f1).compareTo(s2.substring(0, f2)) == 0 &&
                    s1.substring(e1, s1.length() - 1).compareTo(s2.substring(e2,
                            s2.length() - 1)) == 0) {
                    return true;
                }
            }
        }
        return false;
    }
	
	private final void checkPaths(HashSet<Route> paths) {
		int i = 0;
		for (Route path1 : paths) {
			int j = 0;
			for (Route path2 : paths) {
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
				j++;
			}
			i++;
		}
	}

	private final void pathsAlgorithm(Route r1, Route r2, Route er1,
			Route er2) {
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

		if (i < size1 - 1 && i < size2 - 1) {
			ClassMethod cm = n1.get(i);
			distinctPath(r1.cloneSubPath(cm), r2.cloneSubPath(cm), er1, er2);
		}
	}

	// This method should be called when the first nodes of the two routes are
	// the same
	private final void distinctPath(Route r1, Route r2, Route er1, Route er2) {
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

		HashSet<BiCall> allpairs = new HashSet<BiCall>(es.direct);
		allpairs.addAll(es.indirect);

		HashSet<Route> paths = loops.get(h1);
		if (paths != null) {
			log.warning("deterministic.inconsistency.warning",
					er1.routeStr(), er2.routeStr());
			warnings.add(new BiRoute(er1, er2));
			return;
		}

		for (BiCall bc : allpairs) {
			CallEffect ce1 = bc.ce1;
			CallEffect ce2 = bc.ce2;

			// match
			if (ce1.pos() == pos1 && ce2.pos() == pos2) {
				log.warning("deterministic.inconsistency.warning",
						er1.routeStr(), er2.routeStr());
				warnings.add(new BiRoute(er1, er2));
				return;
			}
		}
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
							result.add(new ClassMethod[]{cmn1, cmn2});
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
