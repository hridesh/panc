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

/*
 * @test
 * @summary Ensure Capsule state will not be leaked to other Capsule.
 * @compile/ref=ConfinementCheckerBug.out -XDrawDiagnostics  ConfinementCheckerBug.java
 */

import java.util.ArrayList;

signature Pipeline {
    void collect(ArrayList<Integer> l);
}

capsule ConfinementBug(Pipeline pipeSeg) {
    void run() {
        ArrayList<Integer> list = new ArrayList<Integer>();

        // Send  the collection through the pipeline to accumulate
        // the conents of the pipeline
        pipeSeg.collect(list);

        StringBuilder sb = new StringBuilder();
        // Iterate over the collection. This should be where the
        // confinement violation occures.
        for(int i : list) {
            sb.append(i); sb.append("\n");
        }

        System.out.println(sb.toString());
    }
}

capsule PipeSegment(Pipeline next, int id) implements Pipeline {
	void collect(ArrayList<Integer> l) {
        l.add(id);
        next.collect(l);
    }
}

capsule Sink() implements Pipeline {
    void collect(ArrayList<Integer> l){}
}
