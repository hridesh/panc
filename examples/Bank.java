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
 * Contributor(s): Bryan Shrader
 */

class InvalidTransactionException extends IllegalArgumentException {
	public InvalidTransactionException() {
		super("unable to process request");
	}
}

capsule BankAccount() {
	double balance = 100.0; //state is private and guaranteed to be accessed by only one thread
	void deposit(double money) {
		balance += money;
		System.out.println("depositing $"+money+": total now $"+balance);
	}
	void withdraw(double money) {
		if(balance - money < 0) {
			throw new InvalidTransactionException();
		}
		balance -= money;
		System.out.println("withdrawing $"+money+": total now $"+balance);
	}
}

capsule Client1(BankAccount account) {
	void makeTransactions() {
		account.deposit(25);
		account.withdraw(10);
	}
}

capsule Client2(BankAccount account) {
	void makeTransactions() {
		account.withdraw(25);
		account.deposit(25);
	}
}

capsule Bank (){
	design {
		Client1 c1; Client2 c2;
		BankAccount a;
		c1(a); c2(a);
	}
	void run() {
		c1.makeTransactions(); //Returns immediately: c1 works concurrently.
		c2.makeTransactions(); //Returns immediately: c1 works concurrently.
	}
}

/*
 * This example illustrates an instance where race conditions are avoided.
 * 
 * Client1 and Client2 have the potential to be executed in separate threads.
 * Yet, regardless of the order in which the transactions are executed,
 * the integrity of BankAccount's state is preserved, since only one thread
 * has access thereto (thread safety by means of confinement).
 * 
 */

