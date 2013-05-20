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
package com.sun.tools.panc;

import java.io.PrintWriter;
import java.lang.reflect.*;

/**
 * Bootstrap panc from javac.
 *
 * Having this class allows us to hook into the build infrastructure by
 * with a build-panc target. This class is almost a direct copy of
 * {@link com.sun.tools.javac.Main}, except it creates compiler
 * with the name 'panc' instead of 'javac'.
 */
public class Main {

    /** Unsupported command line interface.
     * @param args   The command line parameters.
     */
    public static void main(String[] args) throws Exception {
      if (args.length > 0 && args[0].equals("-Xjdb")) {
        String[] newargs = new String[args.length + 2];
        Class<?> c = Class.forName("com.sun.tools.example.debug.tty.TTY");
        Method method = c.getDeclaredMethod ("main", new Class<?>[] {args.getClass()});
        method.setAccessible(true);

        System.arraycopy(args, 1, newargs, 3, args.length - 1);
        newargs[0] = "-connect";
        newargs[1] = "com.sun.jdi.CommandLineLaunch:options=-esa -ea:com.sun.tools...";
        newargs[2] = "com.sun.tools.panc.Main";
        method.invoke(null, new Object[] { newargs });
      } else {
        System.exit(compile(args));
      }
    }

    /** Programmatic interface to the Java Programming Language
     * compiler, panc.
     *
     * @param args The command line arguments that would normally be
     * passed to the panc program as described in the man page.
     * @return an integer equivalent to the exit value from invoking
     * panc, see the man page for details.
     */
    public static int compile(String[] args) {
        com.sun.tools.javac.main.Main compiler =
            new com.sun.tools.javac.main.Main("panc");
        return compiler.compile(args).exitCode;
    }



    /** Programmatic interface to the Java Programming Language
     * compiler, panc.
     *
     * @param args The command line arguments that would normally be
     * passed to the panc program as described in the man page.
     * @param out PrintWriter to which the compiler's diagnosti
     * output is directed.
     * @return an integer equivalent to the exit value from invoking
     * panc, see the man page for details.
     */
    public static int compile(String[] args, PrintWriter out) {
        com.sun.tools.javac.main.Main compiler =
            new com.sun.tools.javac.main.Main("panc", out);
        return compiler.compile(args).exitCode;
    }
}

