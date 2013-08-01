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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/**
 * Address book for data stored in comma separated value file.
 * 
 * Assumes the address data is listed firstname, lastname, street, city, state,
 * zipcode
 * 
 * @author Hridesh Rajan
 */
capsule CSVBook (String filename) implements Book {
	
	Address search(AddressRequest r) {
		if(knownAddresses == null) {
			try {
				initDB();
			} catch (FileNotFoundException fe) {
				System.err.println("Unable to find " + new File(filename).getAbsolutePath());
			} catch (Exception e) {
				throw new java.lang.Error("Failed to Initialze Capsule CVSBook", e);				
			}
		}
		return search(r.getFirstname(), r.getLastname());
	}

	List<Address> knownAddresses = null;

	/**
	 * Initialize a list of addresses to lookup data in.
	 * 
	 * @param filename
	 * @throws FileNotFoundException
	 */
	private  void initDB() throws FileNotFoundException {
		knownAddresses = new ArrayList<Address>();
		InputStream in = this.getClass().getResourceAsStream(filename);
		Scanner s = new Scanner(in);

		while (s.hasNext()) {
			String curLine = s.nextLine();

			try {
				knownAddresses.add(parseAddress(curLine));
			} catch (Exception ex) {
				System.err.println("Unable to process " + curLine + "\n" + ex.getMessage());
			}

		}

		// sort the collection by last names and then by first names.
		Collections.sort(knownAddresses, new Address.Comparator());
	}

	/**
	 * Parse a Comma Seperated Value representation of the input file. Assumes the
	 * fields are:
	 * 
	 * @param csv
	 * @return
	 */
	private  Address parseAddress(String csv) {
		Address address = new Address();

		String[] elems = csv.split(",");

		if (elems.length < 6) {
			throw new IllegalArgumentException("Not enough data in the line");
		}

		String firstname, lastname, street, city, state, zipcode;

		firstname = elems[0].trim();
		lastname = elems[1].trim();
		street = elems[2].trim();
		city = elems[3].trim();
		state = elems[4].trim();
		zipcode = elems[5].trim();

		address.setFirstname(firstname);
		address.setLastname(lastname);
		address.setStreet(street);
		address.setCity(city);
		address.setState(state);
		address.setZipcode(zipcode);

		return address;
	}

	/***
	 * Search for a person identified by given first name and last name.
	 * 
	 * @param firstname
	 *         -- first name of the person
	 * @param lastname
	 *         -- last name of the person
	 * @return the address of the person, reference to the copy in the list. Be
	 *         careful about making changes to it.
	 */
	private  Address search(String firstname, String lastname) {

		for (Address address : knownAddresses) {
			if (address.getFirstname().equals(firstname)
					&& address.getLastname().equals(lastname)) {
				return address;
			}
		}
		return null;
	}

}
