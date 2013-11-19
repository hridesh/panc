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
 * Contributor(s): Ganesha Upadhyaya
 */
package examples;

capsule C2 {
	void work() {
		System.out.println("C2");
	}
}

capsule C3 (C2 c2) {
	void work() {
		System.out.println("C3");
		c2.work();
	}
}

capsule C1 (C2 c2) {
	design {
		C3 c31;
		C3 c32;
		C2 c21;
		c31(c2);
		c32(c21);
	}
	void work() {
		System.out.println("C1");
		c31.work();
		c32.work();
	}
}

capsule MultipleSystemDesignDecls {
	design {
		C1 c1;
		C2 c2;
		c1(c2);
	}
	void run() {
		c1.work();
	}
}