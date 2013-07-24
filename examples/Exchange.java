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
 * Contributor(s): Hridesh Rajan adapted original example to Panini
 * 
 * This example is based on code distributed by Brian Egge at 
 * the URL: http://bitbucket.org/brianegge/java-concurrent/src/tip/.
 * A modified and improved version was distributed by Chinmaya at 
 * https://bitbucket.org/chinmaya/java-concurrent_response/. That code
 * is included verbatim at the end of this file for comparison.
 * 
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayDeque;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

signature Exchanger {
	ArrayDeque<String> exchange(ArrayDeque<String> list);
}
capsule Reader (int LIST_SIZE, InputStream in, Exchanger exchanger) {
	BufferedReader reader = null;
    void run() {
    	ArrayDeque<String> list = new ArrayDeque<String>(LIST_SIZE);
    	try {
    		reader = new BufferedReader(new InputStreamReader(
    				new GZIPInputStream(in)));
    	} catch (IOException e) { throw new RuntimeException(e); }

    	String line;
    	try {
          while ((line = reader.readLine()) != null) {
        	  list.add(line.replace("@", "_at_"));
        	  if (list.size() > LIST_SIZE)
                  list = exchanger.exchange(list);
          }
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	}
    }
}
capsule Writer (int LIST_SIZE, OutputStream out) implements Exchanger {
	BufferedWriter writer = null;
    ArrayDeque<String> list = null;
    => { 
    	list = new ArrayDeque<String>(LIST_SIZE);
    	try	{
    		writer = new BufferedWriter(new OutputStreamWriter(
                	new GZIPOutputStream(out)));
    	} catch (IOException e) { throw new RuntimeException(e); }
    }
	ArrayDeque<String> exchange(ArrayDeque inlist) {
		if(inlist == null) {
			try {
				writer.close();
			} catch (IOException e) { throw new RuntimeException(e); }
			return null;
		}
		ArrayDeque<String> temp = list;
		list = inlist;
		process();
		return temp;
	}
	void process() {
		while(!list.isEmpty()) {
			String line = (String) list.pop();
			try {
				writer.write(line);
				writer.write('\n');
			} catch (IOException e) { throw new RuntimeException(e); }
		}
	}
}
capsule Exchange {
    design {
        Reader r; Writer w;
        r(10000, System.in, w); w(10000, System.out);
    }
}

//From: https://bitbucket.org/chinmaya/java-concurrent_response/
//package org.egge.concurrent;
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.OutputStreamWriter;
//import java.util.ArrayDeque;
//import java.util.concurrent.Exchanger;
//import java.util.zip.GZIPInputStream;
//import java.util.zip.GZIPOutputStream;
//
//public class ParallelExchangerTestMod {
//
//      private static final int LIST_SIZE = 10000;
//
//      public static void main(String[] args) throws IOException {
//              Exchanger<ArrayDeque<String>> exchanger1 = new Exchanger<ArrayDeque<String>>();
//
//              Reader reader = new Reader(System.in, exchanger1);
//              Writer writer = new Writer(System.out, exchanger1);
//
//              new Thread(reader, "Reader").start();
//              new Thread(writer, "Writer").start();
//      }
//
//      private static class Reader implements Runnable {
//
//              private final BufferedReader reader;
//              private ArrayDeque<String> list = new ArrayDeque<String>(LIST_SIZE);
//              private final Exchanger<ArrayDeque<String>> exchanger;
//
//              public Reader(InputStream in, Exchanger<ArrayDeque<String>> exchanger)
//                              throws IOException {
//                      this.exchanger = exchanger;
//                      reader = new BufferedReader(new InputStreamReader(
//                                      new GZIPInputStream(in)));
//              }
//
//              @Override
//              public void run() {
//                      String line;
//                      try {
//                              while ((line = reader.readLine()) != null) {
//                                      list.add(line.replace("@", "_at_"));
//                                      if (list.size() > LIST_SIZE)
//                                              list = exchanger.exchange(list);
//                              }
//                              exchanger.exchange(list);
//                              exchanger.exchange(null);
//                      } catch (IOException e) {
//                              throw new RuntimeException(e);
//                      } catch (InterruptedException e) {
//                              Thread.currentThread().interrupt();
//                              return;
//                      }
//              }
//
//      }
//
//
//      private static class Writer implements Runnable {
//
//              private BufferedWriter writer;
//              private ArrayDeque<String> list = new ArrayDeque<String>(LIST_SIZE);
//              private final Exchanger<ArrayDeque<String>> exchanger;
//
//              public Writer(OutputStream out, Exchanger<ArrayDeque<String>> exchanger)
//                              throws IOException {
//                      this.exchanger = exchanger;
//                      writer = new BufferedWriter(new OutputStreamWriter(
//                                      new GZIPOutputStream(out)));
//
//              }
//
//              @Override
//              public void run() {
//                      try {
//                              while (list != null) {
//                                      if (list.isEmpty()) {
//                                              list = exchanger.exchange(list);
//                                              continue;
//                                      }
//                                      String line = list.pop();
//                                      writer.write(line);
//                                      writer.write('\n');
//                              }
//                              writer.close();
//                      } catch (IOException e) {
//                              throw new RuntimeException(e);
//                      } catch (InterruptedException e) {
//                              Thread.currentThread().interrupt();
//                              return;
//                      }
//              }
//
//      }
//}
