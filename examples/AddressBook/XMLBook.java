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
 * Contributor(s): Hridesh Rajan, Sean Mooney
 */

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/***
 * This AddressBook searches for the name in a local 
 * XML file "AddressBook.xml".
 * 
 * @author Hridesh Rajan, Sean Mooney
 */
capsule XMLBook(String filename) implements Book {

	Address search(AddressRequest r) {
		if(listOfAddresses == null) initDB();
		return search(r.getFirstname(), r.getLastname());
	}
	
	NodeList listOfAddresses = null;
	
	private void initDB() {
		try {
			InputStream in = this.getClass().getResourceAsStream(filename);
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			// Document doc = docBuilder.parse (new File(filename));
			Document doc = docBuilder.parse(in);
			// normalize text representation
			doc.getDocumentElement().normalize();

			// System.out.println ("Root element of the doc is " +
			// doc.getDocumentElement().getNodeName());

			listOfAddresses = doc.getElementsByTagName("address");

		} catch (SAXParseException err) {
			System.out.println("** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
			System.out.println(" " + err.getMessage());

		} catch (SAXException e) {
			Exception x = e.getException();
			((x == null) ? e : x).printStackTrace();

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/***
	 * Sequentially search for a person identified by given
	 * first name and last name.
	 * TODO: implement more efficient algorithm for searching.
	 * @param firstname -- first name of the person
	 * @param lastname -- last name of the person
	 * @return the address of the person.
	 */
	private Address search(String firstname, String lastname) {

		Address newAddress = new Address();
		newAddress.setFirstname(firstname);
		newAddress.setLastname(lastname);

		for (int index = 0; index < listOfAddresses.getLength(); index++) {

			Node currentXMLNode = listOfAddresses.item(index);

			if (currentXMLNode.getNodeType() == Node.ELEMENT_NODE) {

				// First match the first name.
				Element currentElement = (Element) currentXMLNode;
				NodeList firstNameList = currentElement.getElementsByTagName("firstname");
				Element firstNameElement = (Element) firstNameList.item(0);
				NodeList textFNList = firstNameElement.getChildNodes();
				String currentFirstName = ((Node) textFNList.item(0)).getNodeValue().trim();
				if (!firstname.equals(currentFirstName))
					continue;

				// Next match the last name.
				NodeList lastNameList = currentElement.getElementsByTagName("lastname");
				Element lastNameElement = (Element) lastNameList.item(0);
				NodeList textLNList = lastNameElement.getChildNodes();
				String currentLastName = ((Node) textLNList.item(0)).getNodeValue().trim();
				if (!lastname.equals(currentLastName))
					continue;

				NodeList streetList = currentElement.getElementsByTagName("street");
				Element streetElement = (Element) streetList.item(0);
				NodeList textStreetList = streetElement.getChildNodes();
				newAddress.setStreet(((Node) textStreetList.item(0)).getNodeValue().trim());

				NodeList cityList = currentElement.getElementsByTagName("city");
				Element cityElement = (Element) cityList.item(0);
				NodeList textCityList = cityElement.getChildNodes();
				newAddress.setCity(((Node) textCityList.item(0)).getNodeValue().trim());

				NodeList stateList = currentElement.getElementsByTagName("state");
				Element stateElement = (Element) stateList.item(0);
				NodeList textStateList = stateElement.getChildNodes();
				newAddress.setState(((Node) textStateList.item(0)).getNodeValue().trim());

				NodeList zipList = currentElement.getElementsByTagName("zipcode");
				Element zipElement = (Element) zipList.item(0);
				NodeList textZipList = zipElement.getChildNodes();
				newAddress.setZipcode(((Node) textZipList.item(0)).getNodeValue().trim());

				return newAddress;
			}
		}
		return null;
	}
}
