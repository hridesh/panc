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
package org.paninij.code;


/**
 * Parallel to {@link com.sun.tools.javac.code.TypeTags}.
 * @since panini-0.9.2
 */
public class TypeTags {

    private TypeTags() {}

    public static final int CAPSULE_WIRING =
            // Start the Panini type tags at 1 after the end of the number
            // of java type tags.
            com.sun.tools.javac.code.TypeTags.TypeTagCount + 1;
}
