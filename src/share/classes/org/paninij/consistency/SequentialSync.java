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
 * This version of the sequential consistency violation detector considers only
 * synchronization.
 */
public class SequentialSync extends SeqConstCheckAlgorithm {
	public SequentialSync(SystemGraph graph, Log log) {
	    super("Sync", graph, log);
	}

	// This method should be called when the first nodes of the two routes are
	// the same.
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

		HashSet<BiCall> direct = es.direct;
		HashSet<BiCall> indirect = es.indirect;

		HashSet<BiCall> allpairs = new HashSet<BiCall>(direct);
		allpairs.addAll(indirect);

		HashSet<Route> paths = loops.get(h1);
		HashSet<Route> pending = new HashSet<Route>();
		if (paths != null) {
			boolean encountered = false;
			for (Route r : paths) {
				if (twoPathsMayConflict(allpairs, r.edges.get(0), e2)) {
					if (check(r, 0, er1, er2)) {
						return;
					}
					encountered = true;
				} else {
					pending.add(r);
				}
			}
			if (encountered) {
				for (Route r : pending) {
					if (check(r, 0, er1, er2)) {
						return;
					}
				}
			}
		}

		if (twoPathsMayConflict(direct, e1, e2)) {
			warnings.add(new BiRoute(r1, r2));
			return;
		}

		if (twoPathsMayConflict(indirect, e1, e2)) {
			check(r1, 1, r1, r2);
			return;
		}
	}

	// this method should be called when the first edge of the first path is
	// asynchronous call.
	private final boolean check(Route r1, int i, Route er1, Route er2) {
		int size1 = r1.size();
		ArrayList<ClassMethod> ns1 = r1.nodes;
		ArrayList<Edge> l1 = r1.edges;

		for (; i < size1 - 1; i++) {
			ClassMethod cm = ns1.get(i);
			Edge ee = l1.get(i);

			if (!synchronousCall(cm, ee.pos)) {
				warnings.add(new BiRoute(er1, er2));
				return true;
			}

			HashSet<Route> paths = loops.get(cm);
			if (paths != null) {
				for (Route r : paths) {
					if (check(r, 0, er1, er2)) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
