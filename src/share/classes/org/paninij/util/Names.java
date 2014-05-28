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
 * Contributor(s):
 */
package org.paninij.util;

import com.sun.tools.javac.util.Name;
import org.paninij.util.PaniniConstants;

/**
 * Names for the panc compiler. Parallel to
 * {@link com.sun.tools.javac.util.Names}.
 * 
 * @author Sean L. Mooney
 * @since panini-0.9.2
 */
public class Names {
	/**
	 * Capsule Initialization method.
	 */
	public final Name PaniniCapsuleInit;
	/**
	 * Name of the 'run' method.
	 */
	public final Name Run;

	/** Name a start method. */
	public final Name Start;

	/**
	 * main
	 */
	public final Name Main;

	public final Name Capsule;
	/**
	 * Name used for a capsule (external) wiring symbol.
	 */
	public final Name Wiring;
	public final Name PaniniFinish;
	public final Name PaniniDuckFuture;
	public final Name PaniniLambdaBody;
	public final Name PaniniDisconnect;
	public final Name PaniniRefCountField;

	/**
	 * Name of the method used to wire internal 'systems' in a capsule.
	 */
	public final Name InternalCapsuleWiring;

	public final Name Org;
	public final Name Paninij;
	public final Name Runtime;
	public final Name UnrunnableCapsuleExceptionClass;

	/**
	 * Construct more names, using an exitings Names table.
	 * 
	 * @param names
	 */
	public Names(com.sun.tools.javac.util.Names names) {
		// Method Names
		PaniniCapsuleInit = names
				.fromString(PaniniConstants.PANINI_CAPSULE_INIT);
		Run = names.fromString("run");
		Start = names.fromString(PaniniConstants.PANINI_START);
		Main = names.fromString("main");

		// Capsule related
		Capsule = names.fromString("Capsule");
		Wiring = names.fromString("Wiring");
		PaniniFinish = names.fromString(PaniniConstants.PANINI_FINISH);
		PaniniLambdaBody = names.fromString(PaniniConstants.PANINI_LAMBDA_BODY);
		PaniniDuckFuture = names.fromString(PaniniConstants.PANINI_DUCK_TYPE);
		PaniniDisconnect = names.fromString(PaniniConstants.PANINI_DISCONNECT);
		InternalCapsuleWiring = names
				.fromString(PaniniConstants.CAPSULE_SYS_WIRE);
		PaniniRefCountField = names
				.fromString(PaniniConstants.PANINI_REF_COUNT);

		// Class and package Names
		Org = names.fromString("org");
		Paninij = names.fromString("paninij");
		Runtime = names.fromString("runtime");
		UnrunnableCapsuleExceptionClass = names
				.fromString(PaniniConstants.UNRUNNABLE_EXCEPTION_CLASS);
	}
}
