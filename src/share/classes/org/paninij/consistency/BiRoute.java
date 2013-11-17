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
 * Contributor(s): Yuheng Long
 */

package org.paninij.consistency;

public class BiRoute {
    final Route r1;
    final Route r2;
  
    public BiRoute(Route r1, Route r2) {
        this.r1 = r1;
        this.r2 = r2;
    }
  
    public final int hashCode() {
        return r1.hashCode() + r2.hashCode();
    }
  
    public final boolean equals(Object obj) {
        if (obj instanceof BiRoute) {
            BiRoute other = (BiRoute)obj;
            return r1.equals(other.r1) && r2.equals(other.r2);
        }
        return false;
    }
}
