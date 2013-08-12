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
 * Contributor(s): Hridesh Rajan, Sean Mooney, Spencer Morrison
 */

import java.util.*;

capsule UI (Book[] books) {
	Address[] search() {
		AddressRequest[] requests = new AddressRequest[] {
				new AddressRequest("Barack", "Obama"),
				new AddressRequest("William", "Clinton"),
				new AddressRequest("George", "Bush"),
				new AddressRequest("Ronald", "Reagan"),
				new AddressRequest("James", "Carter"),
				new AddressRequest("Gerald", "Ford")
		};

		ArrayList<Address> addrs = new ArrayList<Address>();
		for(int j=0; j<requests.length; j++) {
			Address[] results = foreach(Book b : books) b.search(requests[j]);
			for(int i=0; i<books.length; i++){
				if(!results[i].equals(null))
					addrs.add(results[i]);
			}
		}
        return addrs.toArray(new Address[addrs.size()]);
	}
}
