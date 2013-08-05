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

import java.io.PrintWriter;
import java.io.StringWriter;

/*
 * @test
 * @summary Ensures the frontend produces 'panc'
 * instead of javac with the -version flag.
 */

/**
 * @author Sean L. Mooney
 */
public class PancVersionOpt {
    public static void main(String... args) throws Exception {
        new PancVersionOpt().run();
    }

    void run() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        String[] args = new String[] {"-version"};
        int rc = com.sun.tools.panc.Main.compile(args, out);
        if (rc != 0)
            throw new Error("panc failed: rc=" + rc);
        final String actualVersion = sw.toString();
        if (!actualVersion.startsWith(PANC)) {
            throw new Error("\nExpected version sting to start with: " + PANC + "\nFound: " + actualVersion);
        }
        System.out.println(actualVersion);
    }

    public static final String PANC = "panc";
}
