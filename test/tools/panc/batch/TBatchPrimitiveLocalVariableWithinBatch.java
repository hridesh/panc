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
 * @summary Ensure that local variables within the batch message are handled properly.
 *          i.e. they are not considered to be part of the normal method scope and thus
 *          they don't wind up as parameters to the anonymous function.
 *          
 *          A symptom of the test failing is that the compiler complains that the variable
 *          should be final.
 *          
 * @compile TBatchPrimitiveLocalVariableWithinBatch.java
 */
capsule Test {
}

capsule TBatchPrimitiveLocalVariableWithinBatch {
    design {
        Test test;
        test();
    }
    
    void run() {
        int temp = test -> {
            int localVar = 42;
            return localVar;
        };
        System.out.println(temp);
    }
}