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
 * Contributor(s): Loránd Szakács and Yuheng Long
 */

/*
 * @test
 * @summary Ensure Capsule references do not leak to inner classes.
 * @compile/ref=Escape.out -XDrawDiagnostics  Escape.java
 */

interface EscapeInterface {
	public void method();
}

capsule B {
	void escape(EscapeInterface fun){
		fun.method();
	}
}

capsule Escape {
	design{
		B b;
		Escape escapee;
	}

	void run() {
		b.escape(new EscapeInterface(){
			public void method(){
				// a reference to a capsule of type Escape leaked to B
				escapee.nothing();
			}
		});
	}

	void nothing(){};
}
