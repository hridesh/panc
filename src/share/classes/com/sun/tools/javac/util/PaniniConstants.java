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
 * Contributor(s): Eric Lin
 */

package com.sun.tools.javac.util;

public class PaniniConstants {
	public final static String PANINI = "Panini$";
	public final static String PANINI_QUEUE = "PaniniModule";
	public final static String PANINI_MODULE_OBJECTS = "objects";
	public final static String PANINI_MODULE_HEAD = "head";
	public final static String PANINI_MODULE_TAIL = "tail";
	public final static String PANINI_MODULE_SIZE = "size";
	public final static String PANINI_MODULE_QUEUELOCK = "queueLock";
	public final static String PANINI_MODULE_EXTENDQUEUE = "extendQueue";
	public final static String MODULE_CALL_QUEUES = "panini$msgNameQueue";
	public final static String MODULE_ARG_QUEUES = "panini$msgArgsQueue";
	public final static String MODULE_RETURN_QUEUES = "panini$msgDuckQueue";
	public final static String MODULE_METHOD_NAMES = "panini$methodConst";
	public final static String DUCK_INTERFACE_NAME = "Panini$Duck";
	public final static String PANINI_FINISH = "panini$finish";
	public final static String PANINI_MESSAGE = "panini$message";
	public final static String PANINI_GET_NEXT_DUCK = "get$Next$Duck";
	public final static String PANINI_TERMINATE = "panini$terminate";
	public final static String PANINI_METHOD_CONST = "panini$methodConst$";
	public final static String PANINI_MESSAGE_ID = "panini$message$id";
	public final static String PANINI_DUCK_TYPE = "panini$duck$future";

	// To implement the flag in the duck.
	public final static String REDEEMED = "redeemed";
	public final static String VALUE = "value";
}
