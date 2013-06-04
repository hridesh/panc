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
 * Contributor(s): Sean L. Mooney, Lorand Szakacs
 */
package org.paninij.system;

import java.util.List;

import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.util.Log;

/**
 * Takes a system block, with executable Java expressions/stmts
 * and produces a new system block which has no computation
 * and a fixed topology.
 * 
 * @author Sean L. Mooney, Lorand Szakacs
 *
 */
public class SystemInterpreter {

    final Log log;
    
    /**
     * 
     * @param log logger for the compiler
     */
    public SystemInterpreter(Log log) {
        this.log = log;
    }
    
    /**
     * Interpret a system block and ensure it has
     * a fixed topolgy. This method may diverge!
     * @param systemBlock
     * @return
     */
    public JCBlock interpret(final JCBlock systemBlock) {
        //FIXME: Delete when working.
        log.rawWarning(-1, "Interpretting a system block.");
        List<?> classifiedStmts = classifyStmts(systemBlock);
        transform(classifiedStmts);
        JCBlock fixedSystem = execute();
        
        //FIXME: Delete when working.
        log.rawWarning(-1, "System block interpretation finished.");
        
        //TODO: If fixedSystem is null, there are larger problems.
        return (fixedSystem != null ? fixedSystem : systemBlock);
    }

    private JCBlock execute() {
        // TODO Auto-generated method stub
        return null;
    }

    private void transform(List<?> classifiedStmts) {
        // TODO Auto-generated method stub
    }

    private List<?> classifyStmts(JCBlock systemBlock) {
        // TODO Auto-generated method stub
        return null;
    }
}
