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
//panini
capsule M(O o, int fortyTwo){
    void run(){
        System.out.println(fortyTwo);
    }
}

capsule O{
}

system SimpleExpression(String[] args){

    for(int i = 1; i < 10; ++i){
      int fortyTwo = 42;
      int thirtyThree = 33;
      byte by;
      long l;
      long[] la;
      boolean test = false;
      String sA;
      M[42] ms;
      M[fortyTwo] m42;
      int incVal = ++fortyTwo;
      int newLong = la[42];
      int plus = 42 + (fortyTwo = (2 - 32)) - 11;
      plus = 42;
      M m;
      O o;
      o[42](42,42);
      m(o, 42);
      wireall(m, 3, 24);
      assoc(test, test2);
      star(center, more);
      ring(all);
    }
    
    for(int i = 1; i < 10; ++i)
        i = 42;
        
}

