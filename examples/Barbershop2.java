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
 * Contributor(s): Hridesh Rajan, Eric Lin, Rex Fernando
 */

import java.util.Queue;
import java.util.LinkedList;

class Customer {
	private String name;
	public Customer(String name) { this.name = name; }
	public String getName() { return name; }
}

capsule Barber(WaitingRoom r, boolean isSleeping, int totalCustomers) {
	void run() {
		int totalHairCuts = 0; 
		while (totalHairCuts < totalCustomers) {
			if (!isSleeping) {
				Customer c = r.whosNext();
				if(!c.equals(null)) { 
					System.out.println("Barber working on customer " + c.getName());
					totalHairCuts++;
				} else {
					System.out.println("Barber went to sleep");
					isSleeping = true;
				}
			} else yield(1000);
		}
	}
	void wakeIfSleeping() { 
		if(isSleeping) {
			isSleeping = false; 
			System.out.println("Barber Woke up");
		}
	}
}

capsule WaitingRoom(int cap) {
	Queue<Customer> queue = new LinkedList<Customer>();
	void sit(Customer c){
		if (queue.size()<cap) {
			queue.offer(c);
			System.out.println("Customer " + c.getName() + " Sitting in waiting room");
		}
		else 
			System.out.println("Waiting room is full, so " + "customer" + c.getName() + " is leaving");
	}
	Customer whosNext() {
		return queue.poll();
	}
}

capsule Customers(Barber b, WaitingRoom r, String[] customerNames) {
	int idCounter = 0;
	void generate() {
		for (int i = 0; i < customerNames.length; i++) {
			Customer c = new Customer(customerNames[i]);
			System.out.println("Customer " + c.getName() + " wants haircut.");
			b.wakeIfSleeping();
			r.sit(c);
			yield(1000);
		}
	}
}

capsule Barbershop {
	design {
		int numCustomers = 5;
		int numGenerators = 2;
		Barber b;
		WaitingRoom w;
		Customers gs[numGenerators];

		b(w, true, numGenerators * numCustomers);
		w(10);

		gs[0](b,w, new String[]{"Hridesh", "Eric", "Sean", "Yuheng", "Ganesha"});
		gs[1](b,w, new String[]{"Steven", "Sarah", "Bryan", "Lorand", "Rex"});
	}
	
	void run() {
		for(Customers generator: gs)
			generator.generate();
	}
}
