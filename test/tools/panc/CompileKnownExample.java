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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

/*
 */
public abstract class CompileKnownExample {
    final File testDir = new File(System.getProperty("test.src", "."));
    final File examples = new File(testDir, "examples");
    //Don't pollute the src directory with test code.
    final File dest_dir = new File(".");


    void run(File[] files) throws Exception {
        ArrayList<String> pancArgs = new ArrayList<String>();
        pancArgs.add("-d");
        pancArgs.add(dest_dir.getAbsolutePath());
        for(int i = 0; i < files.length; i++) {
            pancArgs.add(files[i].getAbsolutePath());
        }

        panc(pancArgs);
    }

    void panc(ArrayList<String> args) {
        panc(args.toArray(new String[]{}));
    }

    void panc(String... args) {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        try {
            int rc = com.sun.tools.panc.Main.compile(args, out);
            if (rc != 0)
                throw new Error("panc failed: rc=" + rc);
        } finally {
            System.out.println(sw.toString());
        }
    }
}

