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
 * @summary Ensure Capsule state will not be used by other Capsule.
 * @compile/ref=TEscapeThroughAnonClass.out -XDrawDiagnostics TEscapeThroughAnonClass.java
 */

import java.util.ArrayList;

interface Escape {
	void method();
}

capsule B (TEscapeThroughAnonClass a) {
	ArrayList bal = new ArrayList();

	void escape(Escape fun){
		fun.method();
	}
}

capsule TEscapeThroughAnonClass {
	ArrayList aal = new ArrayList();
	int escapeei = 0;

	design {
		TEscapeThroughAnonClass a;
		B b;
		b(a);
	}

	void run() {
		b.escape(new Escape() {
			public void method() {
				escapeei = 2;
			}
		});
	}

	void nothing() {};
}
