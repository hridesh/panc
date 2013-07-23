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

import static com.sun.tools.javac.code.Flags.INTERFACE;
import static com.sun.tools.javac.code.Flags.PRIVATE;
import static com.sun.tools.javac.code.Flags.StandardFlags;
import static com.sun.tools.javac.code.Flags.asFlagSet;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Names;

/**Type checking helper class for the attribution phase of panini code.
 * Companion/parallel to {@link com.sun.tools.javac.comp.Check}
 * @author Sean L. Mooney
 * @since panini-0.9.2
 */
public class Check {

    protected static final Context.Key<Check> checkKey =
            new Context.Key<Check>();

    private final Log log;
    private final Names names;

    public static Check instance(Context context) {
        Check instance = context.get(checkKey);
        if (instance == null)
            instance = new Check(context);
        return instance;
    }

    protected Check(Context context) {
        context.put(checkKey, this);

        log = Log.instance(context);
        names = Names.instance(context);
    }

    /** Check that given modifiers are legal for given symbol and
     *  return modifiers together with any implicit modififiers for that symbol.
     *  Warning: we can't use flags() here since this method
     *  is called during class enter, when flags() would cause a premature
     *  completion.
     *  <p>
     *  Valid symbols that can be check are:
     *  <ul>
     *  <li><code>Kinds.MTH</code> if the name is
     *  {@link org.paninij.util.Names.InterCapsuleWiring} </li>
     *  </ul>
     *
     *  <p>
     *  Follows the default behavior of
     *  {@link com.sun.tools.javac.comp.Check#checkFlags}, which is
     *  throw an unchecked {@link AssertionError} if the symbol is
     *  something the method doesn't know what flags it should use.
     *  Make sure the symbol is method before checking its flags.
     *
     *
     *  @param pos           Position to be used for error reporting.
     *  @param flags         The set of modifiers given in a definition.
     *  @param sym           The defined symbol.
     */
    long checkFlags(DiagnosticPosition pos, long flags, Symbol sym) {
        long mask = 0;
        long implicit = 0;
        switch (sym.kind) {
        case Kinds.MTH:
            if(sym.name == names.panini.InternalCapsuleWiring) {
                implicit = Flags.WIRING_BLOCK_FLAGS;
                mask = Flags.WIRING_BLOCK_FLAGS;
            } else {
                throw new AssertionError();
            }

        break;
        default:
            throw new AssertionError();
        }

        long illegal = flags & StandardFlags & ~mask;
        if (illegal != 0) {
            if ((illegal & INTERFACE) != 0) {
                log.error(pos, "intf.not.allowed.here");
                mask |= INTERFACE;
            }
            else {
                log.error(pos,
                          "mod.not.allowed.here", asFlagSet(illegal));
            }
        }

        return flags & (mask | ~StandardFlags) | implicit;
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
