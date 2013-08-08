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
 * @summary Make sure circular references are detected and warned about.
 * @compile/fail/ref=TCirRefs.out -Werror -XDrawDiagnostics TCirRefs.java
 */

capsule C(D d, E e) { void foo(){}}

capsule D(C c) { void foo(){} }

capsule E(C c) { void foo(){} }

capsule F(F f) { void foo() {} }

capsule M() {
    design {
        C c;
        D d;
        E e;
        c(d, e);
        d(c);
        e(c);
    }
}

capsule N() {
    design {
        F f1, f2;
        f1(f2);
        f2(f1);
    }
}


