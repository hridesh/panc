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
package org.paninij.util;

import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

/**
 * Utility methods for operations on lists
 * @author Sean L. Mooney
 * @since panini-0.9.2
 */
/**
 * @author lorand
 *
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
    
    /**
     * Appends an element to the end of the given list
     * 
     * @param toAppend
     * @param list
     * @return returns a new list containing all the elements of the previous
     *         list to which the new value was appended.
     */
    public static <T> List<T> append(T toAppend, List<T> list) {
        ListBuffer<T> temp = new ListBuffer<T>();
        for (List<T> l = list; l.nonEmpty(); l = l.tail) {
            temp.add(l.head);
        }
        temp.add(toAppend);
        return temp.toList();
    }
    
    /**
     * @param list
     * @param pred
     * @return the first element in the list found to match the given predicate
     */
    public static <T> T findFirst(List<T> list, Predicate<T> pred){
        for (List<T> l = list; l.nonEmpty(); l = l.tail) {
            if(pred.apply(l.head))
                return l.head;
        }
        return null;
    }
    
    
    /**
     * Filters the list by the given predicate
     * @param list
     * @param pred
     * @return returns a list containing all elements from the initial list that
     *         matched the given predicate
     */
    public static <T> List<T> filter(List<T> list, Predicate<T> pred){
        ListBuffer<T> temp = new ListBuffer<T>();
        for (List<T> l = list; l.nonEmpty(); l = l.tail) {
            if(pred.apply(l.head))
                temp.add(l.head);
        }
        return temp.toList();
    }
    
    /**
     * Same as filter, but returns the elements that did not match the predicate.
     * @param list
     * @param pred
     * @return
     */
    public static <T> List<T> filterNot(List<T> list, Predicate<T> pred){
        ListBuffer<T> temp = new ListBuffer<T>();
        for (List<T> l = list; l.nonEmpty(); l = l.tail) {
            if(!pred.apply(l.head))
                temp.add(l.head);
        }
        return temp.toList();
    }
}
