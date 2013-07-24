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
package org.paninij.util.test;

import static org.junit.Assert.assertArrayEquals;
import static org.paninij.util.ListUtils.moveToFirst;

import org.junit.Test;
import org.paninij.util.Predicate;


import com.sun.tools.javac.util.List;

/**
 * @author Sean L. Mooney
 * @since panini-0.9.2
 */
public class TestUtils {

    static final Predicate<Integer> threeIsFirst = new Predicate<Integer>() {
        @Override
        public final boolean apply(Integer t) {
            return t == 3;
        }
    };

    /* //////////////////////////Move to first Tests //////////////////////////
     * Test to assert the moveToFirst function is working correctly.
     */

    @Test
    public void testEmptyList() {
        assertSameOrder(new Integer[0], moveToFirst(List.<Integer>nil(), threeIsFirst));
    }

    @Test(expected=AssertionError.class)
    public void testCanFail() {
        assertSameOrder(new Integer[]{3, 2, 3},
                List.of(3, 2, 2));
    }

    @Test(timeout=10)
    public void testOneElemList() {
        assertSameOrder(new Integer[]{3},
                moveToFirst(List.of(3), threeIsFirst));
        assertSameOrder(new Integer[]{1},
                moveToFirst(List.of(1), threeIsFirst));
    }

   @Test(timeout=10)
   public void testTwoElemList() {
        assertSameOrder(new Integer[]{3, 1},
                moveToFirst(List.of(1, 3), threeIsFirst));
        assertSameOrder(new Integer[]{3, 1},
                moveToFirst(List.of(3, 1), threeIsFirst));
        assertSameOrder(new Integer[]{2, 1},
                moveToFirst(List.of(2, 1), threeIsFirst));
    }

    @Test(timeout=10)
    public void testThreeElemList() {
        Integer[] exp = {3, 1, 2};
        assertSameOrder(exp,
                moveToFirst(List.of(1, 2, 3), threeIsFirst));
        assertSameOrder(exp,
                moveToFirst(List.of(1, 3, 2), threeIsFirst));
        assertSameOrder(exp,
                moveToFirst(List.of(3, 1, 2), threeIsFirst));
    }

    @Test(timeout=10)
    public void testFiveElemList() {
        Integer[] exp = {3, 1, 2, 4, 5};
        assertSameOrder(exp,
                moveToFirst(List.of(3, 1, 2, 4, 5), threeIsFirst));
        assertSameOrder(exp,
                moveToFirst(List.of(3, 1, 2, 4, 5), threeIsFirst));
        assertSameOrder(exp,
                moveToFirst(List.of(3, 1, 2, 4, 5), threeIsFirst));
        assertSameOrder(exp,
                moveToFirst(List.of(3, 1, 2, 4, 5), threeIsFirst));
        assertSameOrder(exp,
                moveToFirst(List.of(3, 1, 2, 4, 5), threeIsFirst));
    }

    static void assertSameOrder(Integer[] exp, List<Integer> l){
        assertArrayEquals(exp, l.toArray());
    }

    ////////////////////////END MOVE TO FIRST //////////////////////////

}
