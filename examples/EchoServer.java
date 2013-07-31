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
 * Contributor(s): Hridesh Rajan, Steven M. Kautz
 */

/*** 
 * An example server in the Panini language that uses a collection of
 * workers organized in a leader-follower style resource pool. This 
 * server goes with the EchoClient also available in this directory.
 */

import java.net.*;
import java.io.*;

capsule Worker(EchoServer l) {
	
	void run() {
		while (true) {
			Socket s = l.getConnection();
			handleConnection(s);
		}
	}
	
	void handleConnection(Socket s) { 
		try {
			PrintWriter out = new PrintWriter(s.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			String clientInput;
			while ((clientInput = in.readLine()) != null) {
				System.out.println("client says: " + clientInput);
				out.println(clientInput);
			}
		} catch (IOException e) { e.printStackTrace(System.err); }
	}

}

capsule EchoServer() {
	
	design {
		Worker workers[10];
		wireall(workers, this);
	}

	ServerSocket ss; 
	
	=> {
		try {
			ss = new ServerSocket(8080);
		} catch (IOException e){ e.printStackTrace(System.err); }
	}
	
	Socket getConnection() {
		Socket s = null;
		try {
			s = ss.accept();
		} catch (IOException e){ e.printStackTrace(System.err); }
		return s;
	}
	
}
