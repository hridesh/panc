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
 * Contributor(s): Hridesh Rajan, Spencer Morrison
 */

import java.util.Scanner;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.ParserException;

/***
 * This AddressBook handler searches for the name in the Iowa State Phone/E-mail
 * Book.
 * 
 * @author Hridesh Rajan, Spencer Morrison
 */
capsule ISUBook() implements Book {

	Address search(AddressRequest r) {
		String name = new String(r.getFirstname() + "%20" + r.getLastname());
		String 	url = "http://www.info.iastate.edu/individuals/search/" + name;
		try {
			Parser par = new Parser(url);
			org.htmlparser.util.NodeList list = par.parse(new HasAttributeFilter("class", "error"));
			if (list.size() > 0) return null;

			list = par.parse(new HasAttributeFilter("class", "sub_title"));
			if (list.size() <= 0) return null;

			Node[] nodes = list.toNodeArray();
			Address addr = new Address();
			addr.setFirstname(r.getFirstname());
			addr.setLastname(r.getLastname());
			String address;
			if (nodes[0].toPlainTextString().equals("In-Session Address:")) {
				par.reset();
				list = par.parse(new HasAttributeFilter("class", "sub_data"));
				nodes = list.toNodeArray();
				address = nodes[0].toString();
			} else {
				par.reset();
				list = par.parse(new HasAttributeFilter("class", "sub_data"));
				nodes = list.toNodeArray();
				address = nodes[2].toString();
			}
			Scanner scan = new Scanner(address);
			scan.useDelimiter(":");
			scan.nextLine();
			scan.next();
			scan.useDelimiter("Tag");
			String hold = scan.next();
			addr.setStreet(hold.substring(1, hold.length()).trim());
			scan.useDelimiter(":");
			scan.next();
			scan.next();
			scan = new Scanner(scan.next().trim());
			scan.reset();
			addr.setCity(scan.next());
			addr.setState(scan.next());
			addr.setZipcode(scan.next().substring(0, 5));
			return addr;
		} catch (ParserException e) {
			e.printStackTrace();
		}
		return null;
	}
}
