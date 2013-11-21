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
 * @summary tests if procedure invocations within batch messages are handled correctly.
 *          
 * @compile TBatchProcedureCallsWithinBatch.java
 */

capsule CapsuleFoo{
    void foo(){
        System.out.println("Foo.foo():void");
    }
    
    int bar(){
        System.out.println("Foo.bar():int");
        return 42;
    }
}

capsule TBatchProcedureCallsWithinBatch {
    design {
        CapsuleFoo capFoo;
        capFoo();
    }
    
    void run() {
        int temp = capFoo -> {
            System.out.println("just to verify that this doesn't get replaced");
            capFoo.foo();
            return capFoo.bar();
        };
        System.out.println(temp);
    }
}