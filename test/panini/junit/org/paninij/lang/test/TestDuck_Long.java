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
package org.paninij.lang.test;

import static org.junit.Assert.*;

import org.junit.Test;

import org.paninij.lang.Long;
import org.paninij.runtime.types.Panini$Duck;

/**
 * Unit tests for the {@link org.paninij.lang.Long}.
 *
 * @author Sean L. Mooney
 */
public class TestDuck_Long {

    @Test(timeout=10)
    public void testDuck$LongWithValue() {
        final long exVal = 100;
        final int mId = 5;
        org.paninij.lang.Long l = new Long(exVal);
        assertEquals("Incorrect Value", exVal, l.longValue());
    }

    @Test(timeout=200)
    public void testDuck$LongFinish() {
        final int mId = 0;
        final long exVal = 100;
        final org.paninij.lang.Long l = new Long(mId);
        assertEquals("Incorrect Message ID", mId, l.panini$message$id());

        FinishingRunnable<java.lang.Long> finisher =
                new FinishingRunnable<java.lang.Long>(l, exVal, 100);
        //start the finisher.
        new Thread(finisher).start();

       assertEquals( "Incorrect Value", exVal, l.longValue() );
    }

    public static void main(String[] args) {
        TestDuck_Long tdl = new TestDuck_Long();
        tdl.testDuck$LongWithValue();
        tdl.testDuck$LongFinish();
    }
}

class FinishingRunnable<T> implements Runnable {
    /**
     * The duck to finish.
     */
    final Panini$Duck<T> duck;
    /**
     * The value to finish with;
     */
    final T finishValue;
    /**
     * Delay until finishing.
     */
    final int finishDelay;
    public FinishingRunnable (Panini$Duck<T> duck, T finishValue, int finishDelay) {
        if(duck == null) {
            throw new IllegalArgumentException();
        }
        if(finishDelay < 0) {
            throw new IllegalArgumentException();
        }

        this.duck = duck;
        this.finishValue = finishValue;
        this.finishDelay = finishDelay;
    }

    public void run() {
        try {
            if(finishDelay > 0){
                Thread.sleep(finishDelay);
            }
            this.duck.panini$finish(finishValue);

        } catch (InterruptedException ie) {
            fail("Interrupted!");
        }
    }
}
