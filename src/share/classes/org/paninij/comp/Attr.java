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

package org.paninij.comp;

import static com.sun.tools.javac.code.Flags.*;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCProcDecl;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Names;

public class Attr {
	// Visitor functions, dispatched here to separate Panini code
    public static void visitProcDef(JCProcDecl tree, Log log){
    	Type restype = ((MethodType)tree.sym.type).restype;
    	if(restype.tsym.isCapsule||tree.sym.getReturnType().isPrimitive()||
    			tree.sym.getReturnType().toString().equals("java.lang.String"))
    	{
    		log.error("procedure.restype.illegal", tree.sym.getReturnType(), tree.sym);
    		System.exit(1);
    	}
    	tree.switchToMethod();
    }

	// Helper functions
    public static JCClassDecl createOwnerInterface(final String interfaceName, 
    		TreeMaker make, Names names) {
    	JCClassDecl typeInterface = 
    			make.ClassDef(
    					make.Modifiers(PUBLIC|INTERFACE|SYNTHETIC), 
    					names.fromString(interfaceName), 
    					List.<JCTypeParameter>nil(), null, 
    					List.<JCExpression>nil(), 
    					List.<JCTree>nil());
    	return typeInterface;
    }
}
