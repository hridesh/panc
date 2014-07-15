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

/**
 * Basic sequential inconsistency detection.
 * This version of the sequential consistency violation detector signals warning
 * when two paths conflict.
 */
public class SequentialBase extends SeqConstCheckAlgorithm {
	public SequentialBase(SystemGraph graph, Log log) {
	    super("Base", graph, log);
	}	

	// This method should be called when the first nodes of the two routes are
	// the same
	protected final void distinctPath(Route r1, Route r2, Route er1,
			Route er2) {
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

		HashSet<BiCall> allpairs = new HashSet<BiCall>(es.direct);
		allpairs.addAll(es.indirect);

		HashSet<Route> paths = loops.get(h1);
		if (paths != null) {
			for (Route r : paths) {
				if (twoPathsMayConflict(allpairs, r.edges.get(0), e2)) {
					warnings.add(new BiRoute(r1, r2));
					return;
				}
			}
		}

		if (twoPathsMayConflict(allpairs, e1, e2)) {
			warnings.add(new BiRoute(r1, r2));
		}
	}
}
