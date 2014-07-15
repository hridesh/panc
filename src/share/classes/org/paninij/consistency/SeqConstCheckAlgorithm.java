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
 * Contributor(s): Sean L. Mooney
 */

package org.paninij.consistency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.tools.JavaFileObject;

import org.paninij.analysis.AnalysisUtil;
import org.paninij.effects.ArrayEffect;
import org.paninij.effects.BiCall;
import org.paninij.effects.CallEffect;
import org.paninij.effects.EffectEntry;
import org.paninij.effects.EffectSet;
import org.paninij.effects.FieldEffect;
import org.paninij.systemgraph.SystemGraph;
import org.paninij.systemgraph.SystemGraph.Edge;
import org.paninij.systemgraph.SystemGraph.Node;

import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;

/**
 * Interface for any class which implements a sequential consistency check
 */
public abstract class SeqConstCheckAlgorithm {
    /**
     * The log object to use for reporting warnings.
     */
    protected final Log log;

    /**
     * The name of the detection algorithm.
     */
    protected final String name;

    private final SystemGraph graph;
    private final HashSet<Route> paths = new HashSet<Route>();
    // all the loops for the capsule methods.
	protected final HashMap<ClassMethod, HashSet<Route>> loops =
		new HashMap<ClassMethod, HashSet<Route>>();
	public HashSet<BiRoute> warnings = new HashSet<BiRoute>();

    /**
     * Constructor.
     * Neither parameter should be null.
     * @param name
     * @param log
     */
    public SeqConstCheckAlgorithm(String name, SystemGraph graph, Log log) {
        assert(log != null);
        assert(name != null);

        this.log = log;
        this.graph = graph;
        this.name = name;
    }

	protected void reportTotalWarnings(HashSet<BiRoute> warnings) {
	    // Do not report counts total warnings for release.
	    // Reenable for benchmarking/testing for papers.
	    // System.out.println(name + " warnings = " + warnings.size());
		/*final int warningsCount = warnings.size();
	    if (warningsCount > 0) {
	        log.warning("deterministic.inconsistency.warning.count",
	        		warnings.size());
	        for (BiRoute r : warnings) {
	            warnSeqInconsistency(r.r1, r.r2);
	        }
	    }*/
	}

	protected void reportTrimmedWarnings(HashSet<BiRoute> warnings) {
	    final int warningsCount = warnings.size();
	    if (warningsCount > 0) {
	        /* log.warning("deterministic.inconsistency.warning.count",
	        		warnings.size()); */
	        for (BiRoute r : warnings) {
	            warnSeqInconsistency(r.r1, r.r2);
	        }
	    }
	}

	/**
	 * Warn a sequential inconsistency was detected.
	 * @param route1
	 * @param route2
	 */
	protected void warnSeqInconsistency(Route route1, Route route2) {
		ArrayList<ClassMethod> nodes = route1.nodes;
		ArrayList<Edge> edges1 = route1.edges;
		ArrayList<Edge> edges2 = route2.edges;

		ClassMethod cm = nodes.get(nodes.size() - 1);
		Edge edge1 = edges1.get(0);
		Edge edge2 = edges2.get(0);

		JCDiagnostic.DiagnosticPosition diag =
				new JCDiagnostic.SimpleDiagnosticPosition(edge1.pos);

		// set the source of the file and record the pre-state
		JavaFileObject previous = log.useSource(edge1.source_file);
		log.warning(diag, "sc.warning", edge1.tree, edge2.tree,
				edge1.line, edge2.line,
				AnalysisUtil.rmDollar(cm.cs.className()));

		// store back the pre-state
		log.useSource(previous);
	}

	private final boolean detect(HashSet<EffectEntry> s1,
			HashSet<EffectEntry> s2, Route path1, Route path2, Route er1,
			Route er2) {
		for (EffectEntry ee1 : s1) {
			for (EffectEntry ee2 : s2) {
				if (ee1 instanceof ArrayEffect) {
					if (ee2 instanceof ArrayEffect) {
						ArrayEffect ae1 = (ArrayEffect)ee1;
						ArrayEffect ae2 = (ArrayEffect)ee2;
						if (ae1.path.equals(ae2)) {
							pathsAlgorithm(path1, path2, er1, er2);
							return false;
						}
					}
					continue;
				}
				if (ee1 instanceof FieldEffect) {
					if (ee2 instanceof FieldEffect) {
						FieldEffect fe1 = (FieldEffect)ee1;
						FieldEffect fe2 = (FieldEffect)ee2;

						if (fe1.f.equals(fe2.f)) {
							pathsAlgorithm(path1, path2, er1, er2);
							return false;
						}
					}
				}
			}
		}
		return true;
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
			ClassMethod cm = n1.get(i);
			EffectSet es = cm.meth.effect;
			HashSet<BiCall> pair = new HashSet<BiCall>(es.direct);
			pair.addAll(es.indirect);

			for (BiCall bc : pair) {
				CallEffect ce1 = bc.ce1;
				CallEffect ce2 = bc.ce2;
				if (ce1.equals(ce2)) {
					if (ee.pos == ce1.pos()) {
						distinctPath(r1.cloneSubPath(cm), r2.cloneSubPath(cm),
								er1, er2);
						break;
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

	protected abstract void distinctPath(Route r1, Route r2, Route er1,
			Route er2);

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

	private final void checkPaths(HashSet<Route> paths) {
		// avoid analyzing the analyzed path.
		HashSet<BiRoute> analyzed = new HashSet<BiRoute>();
		for (Route path1 : paths) {
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

						BiRoute br = new BiRoute(t1, t2);
						if (analyzed.contains(br)) {
							continue;
						}
						analyzed.add(br);

						if ((es1.isBottom && !es2.isPure()) ||
								(!es1.isPure() && es2.isBottom)) {
							pathsAlgorithm(t1, t2, path1, path2);
						} else {
							if (detect(es1.write, es2.write, t1, t2, path1,
									path2)) {
								if (detect(es1.write, es2.read, t1, t2, path1,
										path2)) {
									detect(es1.read, es2.write, t1, t2, path1,
											path2);
								}
							}
						}
					}
				}
			}
		}
	}

	protected final boolean synchronousCall(ClassMethod cm, int pos) {
		HashSet<CallEffect> collected = cm.meth.effect.collected;
		for (CallEffect ce : collected) {
			if (pos == ce.pos()) { return true; }
		}
		return false;
	}

	/** Hook method for sub-types. Sequential consistency checking algorithms
     * should use this method as the entry-point for the algorithm. */
	public final void potentialPathCheck() {
		HashSet<ClassMethod> traversed = new HashSet<ClassMethod>();
		for (Node node : graph.nodes.values()) {
			ClassSymbol cs = node.capsule;
			for (MethodSymbol ms : node.procedures) {
				if (AnalysisUtil.activeRun(cs, ms)) {
					ClassMethod now = new ClassMethod(cs, ms, node);

					if (traversed.contains(now)) { continue; }
					traversed.add(now);

					paths.clear();
					Route al = new Route();
					ConsistencyUtil.traverse(node, null, ms, al, graph, loops,
							paths);
					checkPaths(paths);
				}
			}
		}

        reportTotalWarnings(warnings);
        // HashSet<BiRoute> trimmed = ConsistencyUtil.trim(warnings);
        reportTrimmedWarnings(warnings);
	}

	public static boolean twoPathsMayConflict(HashSet<BiCall> allpairs,
			Edge e1, Edge e2) {
		int pos1 = e1.pos;
		int pos2 = e2.pos;
		Node n1 = e1.toNode;
		Node n2 = e2.toNode;
		for (BiCall bc : allpairs) {
			CallEffect ce1 = bc.ce1;
			CallEffect ce2 = bc.ce2;

			// match
			if (ce1.pos() == pos1 && ce2.pos() == pos2) {
				if (!n1.equals(n2) || ce1.pos() != ce2.pos() ||
						!bc.notsameindex) {
					return true;
				}
			}
		}
		return false;
	}
}
