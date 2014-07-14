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
 * @compile/ref=ConfinementExamples.out -XDrawDiagnostics  ConfinementExamples.java
 */

class TestC {
	TestC next;
	void setNext(TestC next) { this.next = next; }
}

capsule C {
	void test(TestC tc) {  }
}

capsule M (C c) {
	TestC tc = new TestC();
	void mtest() {
		tc.setNext(tc);
		c.test(tc);
	}

	TestC mtest2() {
		return tc;
	}
}

capsule ConfinementExamples {
	design {
		C c; M m;
		m(c);
	}

	void run() {
		m.mtest();
		m.mtest2();
	}
}
