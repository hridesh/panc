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

/*
 * @test
 * @summary Compile the Nbody example. This test is not as thorough as 
 *          most of the tests in the examples folder. It does not check
 *          the expected graph, and does not check the seq consistency
 *          warnings -- can't get the output to match expected out files.
 * @compile Nbody.java
 */

    class DoubleC {
        double v;
        public DoubleC(double v) { this.v = v; }
        public double value() { return v; }
    }

    class IntC {
        int v;
        public IntC(int v) { this.v = v; }
        public int value() { return v; }
    }

capsule Body (Body[] bodies, double x, double y, double vx, double vy, double m, double dt) {
//	include doubles;

    void calcNextV(IntC start) {
    	System.out.println("start calculating the next V");
        for (int i = start.value(); i < bodies.length; i++) {
            double dx = x - bodies[i].getX().value();
            double dy = y - bodies[i].getX().value();

            double dSquared = dx * dx + dy * dy;
            double distance = Math.sqrt(dSquared);
            double mag = dt / (dSquared * distance);

            vx -= dx * bodies[i].getM().value() * mag;
            vy -= dy * bodies[i].getM().value() * mag;

            bodies[i].incrVX(new DoubleC(dx * m * mag));
            bodies[i].incrVY(new DoubleC(dy * m * mag));

        }
    }

    void step() {
        x += dt * vx;
        y += dt * vy;
    }

    DoubleC getX() {return new DoubleC(x);}
    DoubleC getY() {return new DoubleC(y);}
    DoubleC getVX() {return new DoubleC(vx);}
    DoubleC getVY() {return new DoubleC(vy);}
    DoubleC getM() {return new DoubleC(m);}

    void incrVX(DoubleC v) {vx = v.value();}
    void incrVY(DoubleC v) {vy = v.value();}
}

capsule Controller (Body[] bodies, int n) {
    void run() {
        for (int i = 0; i < n; i++) {
            bodies[i].calcNextV(new IntC(i+1));
        }
        for (int i = 0; i < n; i++) {
            bodies[i].step();
        }
    }
}

capsule Nbody {
    design {
        Body b[5];
        Controller c;

        double SOLAR_MASS = 4 * Math.PI * Math.PI;
        double DAYS_PER_YEAR = 365.24;

        // jupiter
        b[0](b, 4.84143144246472090e+00, -1.16032004402742839e+00,
             1.66007664274403694e-03 * DAYS_PER_YEAR, 7.69901118419740425e-03 * DAYS_PER_YEAR,
             9.54791938424326609e-04 * SOLAR_MASS, 0.01);
        // saturn
        b[1](b, 8.34336671824457987e+00, 4.12479856412430479e+00,
             -2.76742510726862411e-03 * DAYS_PER_YEAR, 4.99852801234917238e-03 * DAYS_PER_YEAR,
             2.85885980666130812e-04 * SOLAR_MASS, 0.01);
        // uranus
        b[2](b, 1.28943695621391310e+01, -1.51111514016986312e+01,
             2.96460137564761618e-03 * DAYS_PER_YEAR, 2.37847173959480950e-03 * DAYS_PER_YEAR,
             4.36624404335156298e-05 * SOLAR_MASS, 0.01);
        // neptune
        b[3](b, 1.53796971148509165e+01, -2.59193146099879641e+01,
             2.68067772490389322e-03 * DAYS_PER_YEAR, 1.62824170038242295e-03 * DAYS_PER_YEAR,
             5.15138902046611451e-05 * SOLAR_MASS, 0.01);
        // sun
        b[4](b,0,0,0,0,SOLAR_MASS, 0.01);


        c(b, 5);
    }
}
