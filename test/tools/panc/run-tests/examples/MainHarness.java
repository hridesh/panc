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
 * Contributor(s): Sean L. Mooney
 */

package examples;

import java.lang.reflect.*;

/**
 * Harness to run a panini program.
 * Work-around jtreg expection for the class to run to be located in a source
 * file with the same name. In other words, attempting, a target in a test
 * with <code>@run main TMainMethodGen$serial</code> will fail because it cannot
 * find a source file called TMainMethodGen$serial.java.
 */
public class MainHarness {
    public static void main(String[] args) throws Exception {
        if(args.length < 1) {
            throw new Exception("Please give the name of a Java class to run in the TestHarness.");
        }

        String className = args[0];
        String[] programArgs = null;
        if (args.length > 1) {
            programArgs = new String[args.length-1];
            System.arraycopy(args,1,programArgs,0,programArgs.length);
        }
        testHarness(className, programArgs);
    }

    public static void testHarness(String className, String[] args) throws Exception{
        Class<?> testClass = loadClass(className);
        Method mainMethod = testClass.getDeclaredMethod("main", String[].class);
        mainMethod.invoke(null, (Object) args);
    }

    static Class<?> loadClass(String className) throws ClassNotFoundException, SecurityException {
        Class c = Class.forName(className);
        return c;
    }
}
