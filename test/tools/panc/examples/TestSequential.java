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
 * Contributor(s): Hridesh Rajan
 */

/* 
 * @test
 * @summary Compile the TestSequential example. Do not check the graphs.
 *          May cause spurious failures depending on how the graph output and the Seq inconsistency warnings (from stdout and err) are interleaved.
 * @compile/ref=TestSequential.out -XDrawDiagnostics TestSequential.java
 */

capsule C (D d1, D d2) {
	void mC () {
		d1.mD(); 
		d2.mD(); 
	}
}

capsule D (S s) {
	void mD () {
		s.mS(); 
	}
}

capsule S {
	int si = 0;
	void mS () { si++; }
}

capsule TestSequential {
	design {
		C c; D d1; D d2; S s;
		c(d1,d2); 
		d1(s); d2(s);
	}
	void run() {
		c.mC();
	}
}
