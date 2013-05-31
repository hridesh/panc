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

import java.util.HashSet;

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

    /**
     * Constructor.
     * Neither parameter should be null.
     * @param name
     * @param log
     */
    public SeqConstCheckAlgorithm(String name, Log log) {
        assert( log != null );
        assert( name != null );

        this.log = log;
        this.name = name;
    }

    /**
     * Hook method for sub-types. Sequential consistency
     * checking algorithms should use this method as the
     * entry-point for the algorithm.
     */
	public abstract void potentialPathCheck();

	protected void reportTotalWarnings(HashSet<BiRoute> warnings) {
	    //Do not report counts total warnings for release.
	    //Reenable for benchmarking/testing for papers.
	    //System.out.println(name + " warnings = " + warnings.size());
	}

	protected void reportTrimmedWarnings(HashSet<BiRoute> warnings) {
	    final int warningsCount = warnings.size();
	    if (warningsCount > 0) {
	        log.warning("deterministic.inconsistency.warning.count",
	        		warnings.size());
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
	    log.warning("deterministic.inconsistency.warning",
	            route1.routeStr(), route2.routeStr());
	}
}