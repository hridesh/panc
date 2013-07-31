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
 * Contributor(s): Rex Fernando, Eric Lin
 */

class BooleanC {
	boolean v;
	public BooleanC(boolean v) { this.v = v; }
	public boolean value() { return v; }
}

capsule Barber(WaitingRoom r) {         
	boolean isSleeping = false;
	void wake(){
		isSleeping = false;
		System.out.println("Barber Woke up");
		work();
		while (r.leave().value()) {
			work();
		}
		sleep();
	}

	void sleep(){
		System.out.println("Barber went to sleep");
		isSleeping = true;
	}

	void work(){
		System.out.println("Barber working");
		yield(1000);
	}

	BooleanC isSleeping(){
		return new BooleanC(isSleeping);
	}
}

capsule WaitingRoom(int queue, int cap) {
	BooleanC sit(){
		if (queue<cap) {
			queue++;
			System.out.println("Sitting in waiting room");
			return new BooleanC(true);
		}
		else
			return new BooleanC(false);
	}

	BooleanC leave(){
		if(queue>0){
			queue--;
			return new BooleanC(true);
		}
		else
			return new BooleanC(false);
	}
}

capsule Customers(Barber b, WaitingRoom r) {
	void generate() {
		for (int numCustomers=1; numCustomers <=5; numCustomers++) {
			System.out.println("Customer wants haircut");
			if (!b.isSleeping().value()) {
				trySit();
			} else {
				System.out.println("Customer is waking barber up");
				b.wake();
			}
			yield(1000);
		}
	}

	void trySit(){
		System.out.println("Barber is busy, trying to sit down");
		if(!r.sit().value()) {
			System.out.println("Waiting room is full, so leaving");
		}
	}
}

capsule Barbershop {
	design {
		Barber b;
		WaitingRoom w;
		Customers cs[5];

		b(w);
		w(0, 10);
		wireall(cs, b, w);
	}
	
	void run() {
		for(Customers c: cs)
			c.generate();
	}
}
