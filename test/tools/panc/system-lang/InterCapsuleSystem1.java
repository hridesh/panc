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

/*
 * @test
 * @summary Simple test of declaring a wiring block inside a capsule
 *          instead of requiring a system.
 * @compile InterCapsuleSystem1.java
 */
capsule C1 () {
    int myInt = 5;

    void foo() {
        c12.baz(myInt);
    }

    design {
        C2 c12;
    }

}

capsule C2() {
    public void baz(int i) {}
}
