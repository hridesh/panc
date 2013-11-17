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
 * Contributor(s): Sean L. Mooney, Yuheng Long
 */

package org.paninij.consistency;

import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;

import org.paninij.systemgraph.SystemGraph;
import org.paninij.systemgraph.SystemGraph.Edge;
import org.paninij.systemgraph.SystemGraph.Node;

import java.util.HashMap;
import java.util.HashSet;

public class ConsistencyUtil {
	/** Factory method to map {@link SEQ_CONST_ALG} enums to concrete types of
	 * Checkers.
	 * @param type Checker algorithm to use
	 * @param sysGraph
	 * @param log
	 * @return The checker corresponding to the type. */
	public static SeqConstCheckAlgorithm createChecker(SEQ_CONST_ALG type,
			SystemGraph sysGraph, Log log) {
		switch (type) {
		case BASE:
			return new SequentialBase(sysGraph, log);
		case SYNC:
			return new SequentialSync(sysGraph, log);
		case INORDER:
			return new SequentialInorder(sysGraph, log);
		case TRANS:
			return new SequentialFIFO(sysGraph, log);
			// Keep the compiler happy. Will complain about not all paths
			// returning a values otherwise.
		default:
			return new SequentialFIFO(sysGraph, log);
		}
	}

	/**
	 * Enumerate the types of detection algorithms that exist.
	 * Base, Sync, Inorder and Trans each add one more piece to the detection.
	 *
	 * Instance and lookup logic based on
	 * {@link com.sun.tools.javac.code.Source}.
	 */
	public static enum SEQ_CONST_ALG {
		BASE("base"),
		SYNC("+sync"),
		INORDER("+inorder"),
		TRANS("+trans");

		private static final Context.Key<SEQ_CONST_ALG> seqConstAlgKey
		= new Context.Key<SEQ_CONST_ALG>();

		private static final SEQ_CONST_ALG DEFAULT = TRANS;

		public static SEQ_CONST_ALG instance(Context context) {
			SEQ_CONST_ALG instance = context.get(seqConstAlgKey);
			if (instance == null) {
				Options options = Options.instance(context);
				String seqConstAlg = options.get(Option.XSCLEVEL);
				if(seqConstAlg != null) instance = lookup(seqConstAlg);
				if(instance == null) instance = DEFAULT;
				context.put(seqConstAlgKey, instance);
			}
			return instance;
		}

		private final String name;

		private static HashMap<String, SEQ_CONST_ALG> tab =
			new HashMap<String, ConsistencyUtil.SEQ_CONST_ALG>();
			static {
				for (SEQ_CONST_ALG a: values()) {
					tab.put(a.name, a);
				}
			}

			private static SEQ_CONST_ALG lookup(String name) {
				return tab.get(name);
			}

			private SEQ_CONST_ALG(String name) {
				this.name = name;
			}
	}

	// trim the warnings.
	public static final HashSet<BiRoute> trim(HashSet<BiRoute> warnings) {
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
			// relax the isomorphic condition, consider two nodes are
			// isomorphic, if they have the same Capsule type and same method
			// call
			/*Node n1 = cm1.node;
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
                            s2.length() - 1)) == 0) {*/
			return true;
			/*}
            }*/
		}
		return false;
	}

	public static final void traverse(Node node, Edge e, MethodSymbol ms,
			Route curr, SystemGraph graph,
			HashMap<ClassMethod, HashSet<Route>> loops, HashSet<Route> paths) {
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
			if (e != null) {
				loop.edges.add(e);
			}
			loop.nodes.add(temp);
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
				traverse(ee.toNode, ee, ee.toProcedure, newList, graph, loops,
						paths);
				numEdge++;
			}
		}
		if (numEdge == 0) { paths.add(newList); }
	}
}
