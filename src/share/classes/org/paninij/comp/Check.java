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
package org.paninij.comp;

import static com.sun.tools.javac.code.Flags.StandardFlags;
import static com.sun.tools.javac.code.Flags.asFlagSet;

import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Log;

/**Type checking helper class for the attribution phase of panini code.
 * Companion/parallel to {@link com.sun.tools.javac.comp.Check}
 * @author Sean L. Mooney
 * @since panini-0.9.2
 */
public class Check {

    protected static final Context.Key<Check> checkKey =
            new Context.Key<Check>();

    private final Log log;

    public static Check instance(Context context) {
        Check instance = context.get(checkKey);
        if (instance == null)
            instance = new Check(context);
        return instance;
    }

    protected Check(Context context) {
        context.put(checkKey, this);

        log = Log.instance(context);
    }

    /** Check the flags for capasule parameters
     * @param pos
     * @param flags
     * @return 0. No flags needed.
     */
    public long checkCapsuleParamFlags(DiagnosticPosition pos, long flags) {
        //Make sure there aren't any visibility modifiers on param.
        long illegal = flags & StandardFlags;
        if( illegal != 0) {
            log.error(pos,
                    "mod.not.allowed.here", asFlagSet(illegal));
        }

        return 0;
    }
}