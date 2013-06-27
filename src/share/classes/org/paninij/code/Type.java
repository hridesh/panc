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
 * Contributor(s):
 */
package org.paninij.code;

import com.sun.tools.javac.code.Symbol.CapsuleExtras;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.util.List;

import static org.paninij.code.TypeTags.*;

/**
 * This class represents Panini types.
 *
 * Note: Disambiguating between Panini Types and Javac Types leads to lots
 * of fully qualified type names.
 *
 * @author Sean L. Mooney
 * @since panini-0.9.2
 */
public abstract class Type  {

    public static class WiringType extends com.sun.tools.javac.code.Type {

        List<com.sun.tools.javac.code.Type> wiringParamTypes;

        public WiringType(List<com.sun.tools.javac.code.Type> wiringTypes, ClassSymbol tsym) {
            super(CAPSULE_WIRING, tsym);
            this.wiringParamTypes = wiringTypes;
        }

        public List<com.sun.tools.javac.code.Type> getWiringTypes () {
            return wiringParamTypes;
        }

        @Override
        public List<com.sun.tools.javac.code.Type>        getParameterTypes() { return wiringParamTypes; }

        public String toString() {
            return "(" + wiringParamTypes + ")";
        }
    }

}
