package org.paninij.analysis;

import java.awt.AWTEvent;
import java.awt.Component;

public interface Blocking {
	static final String[] io_methods = {
		"int java.io.InputStream.read()",
		"int java.io.InputStream.read(byte[])",
		"int java.io.InputStream.read(byte[],int,int)",
		"int java.io.PushbackInputStream.read()",
		"int java.io.PushbackInputStream.read(byte[],int,int)",
		"int java.io.FileInputStream.read()",
		"int java.io.FileInputStream.read(byte[])",
		"int java.io.FileInputStream.read(byte[],int,int)",
		"void java.io.DataInput.readFully(byte[])",
		"void java.io.DataInput.readFully(byte[],int,int)",
		"void java.io.PipedOutputStream.write(byte[],int,int)",
		"int java.io.DataInputStream.read(byte[])",
		"int java.io.DataInputStream.read(byte[],int,int)",
		"int java.io.RandomAccessFile.read()",
		"int java.io.RandomAccessFile.read(byte[],int,int)",
		"int java.io.RandomAccessFile.read(byte[])",
		"void java.io.RandomAccessFile.readFully(byte[])",
		"void java.io.RandomAccessFile.readFully(byte[],int,int)",
		"boolean java.io.RandomAccessFile.readBoolean()",
		"byte java.io.RandomAccessFile.readByte()",
		"int java.io.RandomAccessFile.readUnsignedByte()",
		"short java.io.RandomAccessFile.readShort()",
		"int java.io.RandomAccessFile.readUnsignedShort()",
		"char java.io.RandomAccessFile.readChar()",
		"int java.io.RandomAccessFile.readInt()",
		"long java.io.RandomAccessFile.readLong()",
		"float java.io.RandomAccessFile.readFloat()",
		"double java.io.RandomAccessFile.readDouble()",
		"java.lang.String java.io.RandomAccessFile.readLine()",
		"java.lang.String java.io.RandomAccessFile.readUTF()",
		"int java.io.FilterInputStream.read()",
		"int java.io.FilterInputStream.read(byte[])",
		"int java.io.FilterInputStream.read(byte[],int,int)",
		"int java.io.SequenceInputStream.read()",
		"int java.io.SequenceInputStream.read(byte[],int,int)",
		"int java.io.PipedReader.read()",
		"int java.io.PipedReader.read(byte[],int,int)",
		"int java.io.LineNumberInputStream.read()",
		"int java.io.LineNumberInputStream.read(byte[],int,int)",
		"void java.io.PipedWriter.write(byte[],int,int)",
		"int java.io.PipedInputStream.read()",
		"int java.io.PipedInputStream.read(byte[],int,int)"
	};
	
	static final String[] util_methods = {
		"int javax.sound.sampled.TargetDataLine.read(byte[],int,int)",
		"void javax.sound.sampled.DataLine.drain()",
		"int javax.sound.sampled.SourceDataLine.write(byte[],int,int)",
		"int javax.sound.sampled.AudioInputStream.read(byte[])",
		"void java.net.DatagramSocket.receive(java.net.DatagramPacket)",
		"java.net.Socket java.net.ServerSocket.accept()",
		"boolean java.awt.SecondaryLoop.enter()",
		"boolean java.awt.DefaultKeyboardFocusManager.sendMessage(java.awt.Component,java.awt.AWTEvent)",
		"void java.awt.SequencedEvent.dispatch()",
		"void com.sun.jmx.snmp.daemon.CommunicatorServer.waitIfTooManyClients()",
		"void com.sun.jmx.snmp.daemon.CommunicatorServer.waitClientTermination()",
		"java.lang.String java.util.Scanner.next()",
		"boolean java.util.Scanner.hasNext()",
		"void javax.swing.SwingUtilities.invokeAndWait(java.lang.Runnable)"
	};
	
	// LockSupport methods which uses sun.misc.Unsafe.park()"
	static final String[] locksupport_methods = {
		"void java.util.concurrent.lock.LockSupport.park()",
		"void java.util.concurrent.lock.LockSupport.parkNanos(long)",
		"void java.util.concurrent.lock.LockSupport.parkUntil(long)",
		"void java.util.concurrent.lock.LockSupport.park(Object)",
		"void java.util.concurrent.lock.LockSupport.parkNanos(Object,long)",
		"void java.util.concurrent.lock.LockSupport.parkUntil(Object,long)"
	};
	
	// methods which use LockSupport blocking calls
	static final String[] lock_methods = {
		// TODO: not able to track these methods.
		// Note that, LockSupport blocking calls are used by
		// explicit concurrent libraries, hence, programmer 
		// without concurrency in mind may not use them.
	};
	
	static final String[] thread_methods = {
		"void java.lang.Thread.sleep(long)",
		"void java.lang.Thread.sleep(long,int)",
		"void java.lang.Thread.yield()",
		"void org.paninij.runtime.PaniniCapsuleSequential.yield(long)"
	};
}
