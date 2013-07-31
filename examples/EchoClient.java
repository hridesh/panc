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
 * An example client in the Panini language that goes with the EchoServer. 
 */
import java.net.*;
import java.io.*;

capsule EchoClient() {
	
	Socket echoSocket = null; 
	PrintWriter out = null;
	BufferedReader in = null; 
	BufferedReader stdIn = null;
	
	void run() {
		try {
			String userInput;
			open();
			out.println("Hello Server!");
			System.out.println("Server replied: " + in.readLine());
			out.println("" + System.currentTimeMillis() + ".");			
			System.out.println("Server replied: " + in.readLine());
			out.println("Good bye.");			
			System.out.println("Server replied: " + in.readLine());
			close();
		} catch (IOException e) { 
			e.printStackTrace(System.err); 
		}
	}

	private void open() {
		try {
			echoSocket = new Socket("localhost", 8080);
			out = new PrintWriter(echoSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
			stdIn = new BufferedReader(new InputStreamReader(System.in));
		} catch (UnknownHostException e) { 
			e.printStackTrace(System.err); 
		} catch (IOException e) { 
			e.printStackTrace(System.err); 
		}
	}
	
	private void close() {
		try {
			out.close();
			in.close();
			stdIn.close();
			echoSocket.close();
		} catch (IOException e) { 
			e.printStackTrace(System.err); 
		}
	}
}