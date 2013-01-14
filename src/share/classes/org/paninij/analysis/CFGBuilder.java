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
 * Contributor(s): Rex Fernando
 */

package org.paninij.analysis;

import com.sun.tools.javac.util.*;

import com.sun.tools.javac.tree.JCTree.*;

public class CFGBuilder {
    private static CFGNodeBuilder nodeBuilder = new CFGNodeBuilder();
    private static CFGNodeConnector nodeConnector = new CFGNodeConnector();

    public static void setNames(Names n) { nodeBuilder.names = n; }

    public static CFG buildCFG(JCModuleDecl module, JCMethodDecl m) {
        if (m.sym.cfg != null) 
            return m.sym.cfg;
        else {
            CFG cfg = new CFG();
            nodeBuilder.buildNodes(module, m, cfg);
            nodeConnector.connectNodes(m, cfg);
            m.sym.cfg = cfg;
            return cfg;
        }
    }
}