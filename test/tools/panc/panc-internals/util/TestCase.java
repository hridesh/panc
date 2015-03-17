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
package util;

//Derivived from test/tools/javac/parser/JavacParserTest

/**
 * @since panini-0.9.2
 */
public class TestCase {
    public void assertEquals(String message, int exp, int act) {
        if (exp != act) {
            fail(message + ": " + exp + " != " + act);
        }
    }

    public void assertTrue(String message, boolean pred) {
        if(!pred)
            fail(message);
    }

    public void assertFalse(String message, boolean pred) {
        if(pred) {
            fail(message);
        }
    }

    public void assertEquals(String message, int exp, long act) {
        if (exp != act) {
            fail(message + ": " + exp + " != " + act);
        }
    }

    public void assertEquals(String message, Object expected, Object actual) {
        if (expected != null && actual != null && !expected.equals(actual)) {
            fail(message + " Expected " + expected + ", found " + actual);
        }
        if (expected == null && actual != null) {
            fail(message + " Expected " + expected + ", found " + actual);
        }
    }

    public void assertNotNull(Object o) {
        if (o == null) {
            fail("Unexpected Null Object");
        }
    }

    public void fail() {
        fail("test failed");
    }

    public void fail(String message) {
        throw new RuntimeException(message);
    }
}
