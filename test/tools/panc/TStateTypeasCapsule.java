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
 * Contributor(s): Eric Lin
 */

/* @test
 * @summary Ensure that declaring states with capsule type gives a proper error message.
 * @compile/fail/ref=TStateTypeasCapsule.out -XDrawDiagnostics TStateTypeasCapsule.java
 */

capsule C11 {
    void consume() {
        System.out.println("C11");
    }
}

capsule C1 {
    C11 c11;
    void consume() {
        c11.consume();
        System.out.println("C1");
    }
}
