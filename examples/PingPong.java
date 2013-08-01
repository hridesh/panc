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
 * Contributor(s): Yuheng Long
 */

capsule Pong(Ping ping) {
	boolean done = false;

	void act(int message) {
		if (!done) {
			ping.act(message);
			if (message % 1000 == 0) {
				System.out.println("message = " + message);
			}
		}
	}

	void done() { done = true; }
}

capsule Ping(Pong pong, int total) {
	boolean done = false;

	void act(int message) {
		if (message > 0) {
			publishPing();
		} else {
			pong.done();
			done = true;
		}
	}

	void done() { done = true; }

	private void publishPing() {
		total--;
		pong.act(total);
	}
}

capsule PingPong () {
	design {
		Ping ping;
		Pong pong;

		ping(pong, 1000000);
		pong(ping);
	}
	void run() {
		ping.act(1000000);
	}
}
