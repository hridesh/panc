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
 * Contributor(s): Hridesh Rajan
 */

/* @test
 * @summary Test compile the address book example.
 * @library htmlparser.jar
 * @compile AddressBook.java
 */

/***
 * The design of this application consists of a collection of address books 
 * and a UI capsule. The UI capsule initiates the address search by broadcasting
 * the address to all books. A different kind of address book can be added 
 * without affecting the UI capsule, so in that sense these capsules 
 * are decoupled.
 */
capsule AddressBook {
    design {
        CSVBook csv; XMLBook xml; DexBook dex; YellowBook yellow; ISUBook isu;
        UI ui;
        csv("AddressBook.csv");
        xml("AddressBook.xml");
        ui(new Book[]{csv,xml,dex,yellow,isu});
    }
}
