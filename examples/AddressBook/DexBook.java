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

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.ParserException;

/***
 * This AddressBook searches for the name in the white pages provided by
 * the DEX company.
 * 
 * @author Hridesh Rajan, Spencer Morrison
 */
capsule DexBook() implements Book {

	Address search(AddressRequest request) {

		String url = "http://dexknows.whitepages.com/search/FindPerson?extra_listing=mixed&form_mode=opt_a&post_back=0&firstname_begins_with=1&firstname="
				+ request.getFirstname() + "&name=" + request.getLastname() + "&city_zip=&localtime=survey";
		Parser par;
		Address newAddress = new Address();
		newAddress.setFirstname(request.getFirstname());
		newAddress.setLastname(request.getLastname());
		try {
			par = new Parser(url);
			org.htmlparser.util.NodeList list;
			list = par.parse(new HasAttributeFilter("class", "street-address"));
			if (list.size() <= 0) return null;
			Node[] nodes = list.toNodeArray();
			newAddress.setStreet(nodes[0].toPlainTextString());

			par.reset();
			list = par.parse(new HasAttributeFilter("class", "locality"));
			nodes = list.toNodeArray();
			newAddress.setCity(nodes[0].toPlainTextString() + ",");

			par.reset();
			list = par.parse(new HasAttributeFilter("class", "region"));
			nodes = list.toNodeArray();
			newAddress.setState(nodes[0].toPlainTextString());

			par.reset();
			list = par.parse(new HasAttributeFilter("class", "postal-code"));
			nodes = list.toNodeArray();
			newAddress.setZipcode(nodes[0].toPlainTextString().substring(0, 5));
			return newAddress;
		} catch (ParserException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
