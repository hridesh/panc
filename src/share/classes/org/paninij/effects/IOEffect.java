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

package org.paninij.effects;

import javax.tools.JavaFileObject;

import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;

public class IOEffect implements CallEffect {
	// the stmt that cause this effect
	public final JCMethodInvocation tree;
	// the source file that contains this effect
	public final JavaFileObject source_file;

	public IOEffect(JCMethodInvocation tree, JavaFileObject source_file) {
		this.tree = tree;
		this.source_file = source_file;
	}

	public void printEffect() {
		System.out.println("IO effect");
	}

	public int hashCode() {
		return 1;
	}

	public boolean equals(Object obj) {
		return obj instanceof IOEffect;
	}

	public String effectToString() {
		return "I";
	}

	public int pos() { return -1; }

	public JCMethodInvocation call_stmt() { return tree; }
	public JavaFileObject source_file() { return source_file; }
}
