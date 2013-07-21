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
package org.paninij.util;

import com.sun.tools.javac.util.List;

/**
 * Utility methods for operations on lists
 * @author Sean L. Mooney
 * @since panini-0.9.2
 */
public class ListUtils {

    private ListUtils() {}

    /**
     * Move the first element of the list that matches the predicate to
     * the front of the list.
     * @param capsule
     */
    public static <T> List<T> moveToFirst(List<T> capsule, Predicate<T> pred) {
       //nothing to reorder.
        if(capsule.size() < 2){
            return capsule;
        }
        if (pred.apply(capsule.head)) {
            return capsule;
        }

        List<T> head = capsule;
        List<T> curr = head.tail;
        List<T> prev = head;
        while(curr.nonEmpty()) {
            if (pred.apply(curr.head)) {
                prev.tail = curr.tail;
                curr.tail = head;
                head = curr;
                break;
            } else {
                prev = curr;
                curr = curr.tail;
            }
        }
        return head;
    }
}
