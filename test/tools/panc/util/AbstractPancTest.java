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
package util;

import java.net.URI;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.tools.javac.api.JavacTaskImpl;

/**
 * Base class for any tests which need to check elements of
 * the compilation phase, and not just that a particular
 * test compiled, or failed with an expected error message.
 * @author Sean L. Mooney
 * @since panini-0.9.2
 */
public abstract class AbstractPancTest extends TestCase{

    /**Reference to the tool being tested.
     */
    private final JavaCompiler tool;

    protected AbstractPancTest() {
        tool = ToolProvider.getSystemJavaCompiler();
    }

    /**
     * Get the tasks to be done to compile the 'code'.
     *
     * <p>
     * Once the tasks are created the results of each compilation phase
     * can be accessed with the methods
     * <ul>
     * <li>{@link JavacTaskImpl#parse}</li>
     * <li>{@link JavacTaskImpl#enter}</li>
     * <li>{@link JavacTaskImpl#analyze}</li>
     * <li>{@link JavacTaskImpl#generate}</li>
     * </ul>
     * @param code source code to start with
     * @return reference to an object that will execute the compilation phases.
     */
    public JavacTaskImpl getTasks(String code) {
        return (JavacTaskImpl) tool.getTask(null, null, null, null, null,
                Arrays.asList(new SourceCodeFileObject(code)));
    }

    /**
     * Create a file object representing a piece of source code.
     * Used to create test objects from 'code' in the test.
     * @author Sean L. Mooney
     * @since panini-0.9.2
     */
    protected static class SourceCodeFileObject extends SimpleJavaFileObject {

        private String text;

        public SourceCodeFileObject(String text) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }
}
