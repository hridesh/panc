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

import com.sun.tools.javac.util.Log;

/**
 * Interface for any class which implements a sequential consistency check
 */
public abstract class SeqConstCheckAlgorithm {

    /**
     * The log object to use for reporting warnings.
     */
    protected final Log log;

    public SeqConstCheckAlgorithm(Log log) {
        assert( log != null );
        this.log = log;
    }

	public abstract void potentialPathCheck();

	/**
	 * Warn a sequential inconsistency was detected.
	 * @param route1
	 * @param route2
	 */
	protected void warnSeqInconsistency(String route1, String route2) {
	    log.warning("deterministic.inconsistency.warning",
	            route1, route2);
	}
}

