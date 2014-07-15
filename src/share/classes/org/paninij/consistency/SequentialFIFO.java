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
 * This version of the sequential consistency violation detector considers the
 * FIFO of the Capsule message queue and the transitive in order delivery.
 */
public class SequentialFIFO extends SeqConstCheckAlgorithm {
	public SequentialFIFO(SystemGraph graph, Log log) {
	    super("Trans", graph, log);
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
		int size2 = ns2.size();

		HashSet<BiCall> direct = es.direct;
		HashSet<BiCall> indirect = es.indirect;

		HashSet<BiCall> allpairs = new HashSet<BiCall>(direct);
		allpairs.addAll(indirect);

		int j = 0;
		HashSet<Route> paths = loops.get(h1);
		if (paths != null) {
			boolean changed;
			do {
				changed = false;
				boolean first = true;
				for (Route r : paths) {
					int temp = j;
					if (!first || twoPathsMayConflict(allpairs,
							r.edges.get(0), e2)) {
						j = check(r, 0, r2, j, er1, er2);

						if (j >= size2 - 1) {
							warnings.add(new BiRoute(r1, r2));
							return;
						}
						if (j != temp) {
							changed = true;
						}
					}
				}
				first = false;
			} while(changed);
		}

		if (j >= size2 - 1) {
			warnings.add(new BiRoute(r1, r2));
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

		// boolean existReverse = false;
		if (twoPathsMayConflict(direct, e1, e2)) {
			check(r1, 1, r2, 1, er1, er2);
			return;
		}

		// if (existReverse) { return; }
		if (twoPathsMayConflict(indirect, e1, e2)) {
			if (2 < size1) {
				int i = 1;
				ClassMethod cm = ns1.get(i);
				Edge ee = l1.get(i);
				while (i < size1 - 1 && synchronousCall(cm, ee.pos)) {
					i++;
					cm = ns1.get(i);
					ee = l1.get(i);
				}
				if (i < size1 - 1) {
					check(r1, i + 1, r2, j, er1, er2);
				}
			}
			return;
		}
	}

	// this method should be called when the first edge of the first path is
	// asychronous call. TODO(yuhenglong): this logic should be studied more
	// carefully to reduce false positive.
	private final int check(Route r1, int i, Route r2, int j, Route er1,
			Route er2) {
		int size1 = r1.size();
		int size2 = r2.size();
		ArrayList<ClassMethod> ns1 = r1.nodes;
		ArrayList<Edge> l1 = r1.edges;
		ArrayList<ClassMethod> ns2 = r2.nodes;

		for (; i < size1 - 1;) {
			ClassMethod cm = ns1.get(i);
			for (; j < size2 - 1; j++) {
				ClassMethod cm2 = ns2.get(j);
				if (cm.node.equals(cm2.node)) {
					i++;
					j++;
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
				warnings.add(new BiRoute(r1, r2));
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

				Edge ee = l1.get(i - 1);
				ClassMethod cmm1 = ns1.get(i - 1);
				while (i < size1 - 1 && synchronousCall(cmm1, ee.pos)) {
					i++;
					if (i < size1 - 1) {
						cmm1 = ns1.get(i - 1);
						ee = l1.get(i - 1);
					}
				}
			}
		}
		return j;
	}
}
