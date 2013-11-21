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
 * Contributor(s): Lorand Szakacs
 */

/*
 * @test
 * @summary tests to see that a duck type isn't duplicate in case we have
 *          two batch messages with the same return type within the same capsule.
 *          
 * @compile TBatchDuplicateDuck.java
 */

capsule CapsuleFoo{
}

capsule TBatchProcedureCallsWithinBatch {
    design {
        CapsuleFoo capFoo;
        capFoo();
    }
    
    void run() {
        int t1 = capFoo -> {
            return 1;
        };
        
        int t2 = capFoo -> {
            return 2;
        };
    }
}