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
		int pos1 = e1.pos;
		int pos2 = e2.pos;

		HashSet<Route> paths = loops.get(h1);
		if (paths != null) {
			for (Route r : paths) {
				check(r, 0, er1, er2);
			}
		}

		HashSet<BiCall> direct = es.direct;
		// boolean existReverse = false;
		for (BiCall bc : direct) {
			CallEffect ce1 = bc.ce1;
			CallEffect ce2 = bc.ce2;

			// match
			if (ce1.pos() == pos1 && ce2.pos() == pos2) {
				if (ce1.pos() != ce2.pos() || !bc.notsameindex) {
					warnings.add(new BiRoute(er1, er2));
					return;
				}
			} /*else if (ce1.pos() == pos2 && ce2.pos() == pos1) {
				// return;
				existReverse = true;
			}*/
		}
		// if (existReverse) { return; }
		HashSet<BiCall> indirect = es.indirect;
		for (BiCall bc : indirect) {
			CallEffect ce1 = bc.ce1;
			CallEffect ce2 = bc.ce2;

			// match
			if (ce1.pos() == pos1 && ce2.pos() == pos2) {
				if (ce1.pos() != ce2.pos() || !bc.notsameindex) {
					check(r1, 1, er1, er2);
					return;
				}
			}
		}
	}

	// this method should be called when the first edge of the first path is
	// asynchronous call.
	private final void check(Route r1, int i, Route er1, Route er2) {
		int size1 = r1.size();
		ArrayList<ClassMethod> ns1 = r1.nodes;
		ArrayList<Edge> l1 = r1.edges;

		for (; i < size1 - 1; i++) {
			ClassMethod cm = ns1.get(i);
			Edge ee = l1.get(i);

			if (!synchronousCall(cm, ee.pos)) {
				warnings.add(new BiRoute(er1, er2));
				return;
			}
		}
	}
}
