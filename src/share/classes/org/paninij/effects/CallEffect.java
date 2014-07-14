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

public interface CallEffect extends EffectEntry {
	// The position of the method call in the source code. It serves as a unique
	// identifier for the method call within the capsule.
	public int pos();

	public JCMethodInvocation call_stmt();

	public JavaFileObject source_file();
}
