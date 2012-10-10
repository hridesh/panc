package org.paninij.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GlobalPool{
	
	ExecutorService pool;
	public GlobalPool(){
		pool = Executors.newCachedThreadPool();
	}
	
	public ExecutorService getPool(){
		return pool;
	}
}
