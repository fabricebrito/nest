/*
 * Copyright (C) 2011 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.nest.util;

import junit.framework.TestCase;


/**
 * MathUtils Tester.
 *
 * @author lveci
 */
public class TestMathUtils extends TestCase {

    final int numItr = 20000000;

    public TestMathUtils(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testHanning() {
        int windowLength = 5;
        double w0 = MathUtils.hanning(-2.0, windowLength);
        double w1 = MathUtils.hanning(-1.0, windowLength);
        double w2 = MathUtils.hanning( 0.0, windowLength);
        double w3 = MathUtils.hanning( 1.0, windowLength);
        double w4 = MathUtils.hanning( 2.0, windowLength);
        assertTrue(Double.compare(w0, 0.2500000000000001) == 0);
        assertTrue(Double.compare(w1, 0.75) == 0);
        assertTrue(Double.compare(w2, 1.0) == 0);
        assertTrue(Double.compare(w3, 0.75) == 0);
        assertTrue(Double.compare(w4, 0.2500000000000001) == 0);
    }

    public void testInterpolationSinc() {

        double y0 = (-2.0 - 0.3)*(-2.0 - 0.3);
        double y1 = (-1.0 - 0.3)*(-1.0 - 0.3);
        double y2 = (0.0 - 0.3)*(0.0 - 0.3);
        double y3 = (1.0 - 0.3)*(1.0 - 0.3);
        double y4 = (2.0 - 0.3)*(2.0 - 0.3);
        double mu = 0.3;
        double y = MathUtils.interpolationSinc(y0, y1, y2, y3, y4, mu);

        double yExpected = -0.06751353045007912;
        assertTrue(Double.compare(y, yExpected) == 0);
    }

     public void testMathCos() {
        for(int i=0; i < numItr; ++i) {
            double val = Math.cos(i);
        }
    }

    public void testFastMathCos() {
        for(int i=0; i < numItr; ++i) {
           // double val = FastMath.cos(i);
        }
    }

    public void testMathMin() {
        for(int i=0; i < numItr; ++i) {
            double val = Math.min(i, 500);
        }
    }

    public void testFastMathMin() {
        for(int i=0; i < numItr; ++i) {
          //  double val = FastMath.min(i, 500);
        }
    }

    public void testMathFloor() {
        for(int i=0; i < numItr; ++i) {
            double val = Math.floor(i);
        }
    }

    public void testFastMathFloor() {
        for(int i=0; i < numItr; ++i) {
           // double val = FastMath.floor(i);
        }
    }

    public void testMathAbs() {
        for(int i=0; i < numItr; ++i) {
            double val = Math.abs(i);
        }
    }

    public void testFastMathAbs() {
        for(int i=0; i < numItr; ++i) {
           // double val = FastMath.abs(i);
        }
    }
}