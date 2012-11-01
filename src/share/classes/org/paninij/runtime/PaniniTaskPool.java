package org.paninij.runtime;

final class PaniniTaskPool extends Thread {
		static final synchronized void setSize(int size){
			poolSize = size;
			_getInstance = new PaniniTaskPool[size];
			for(int i=0;i<_getInstance.length;i++){
				_getInstance[i] = new PaniniTaskPool();
			}
		}
		
		static final synchronized PaniniTaskPool add(PaniniModuleTask t) {
			// TODO: see load balancing
			int currentPool = nextPool;
			if(nextPool>=poolSize-1)
				nextPool = 0;
			else
				nextPool++;
			_getInstance[currentPool]._add(t);
			if (!_getInstance[currentPool].isAlive()) {
				_getInstance[currentPool].start();
			}
			return _getInstance[currentPool];
		}
		static final synchronized void remove(PaniniTaskPool pool, PaniniModuleTask t) {
			pool._remove(t);
		}
		
		private final synchronized void _add(PaniniModuleTask t){
			if(_headNode==null){
				_headNode = t;
				t.next = t;
			}else{
				t.next = _headNode.next;
				_headNode.next = t;
			}
		}
		
		private final synchronized void _remove(PaniniModuleTask t){
			PaniniModuleTask current = _headNode;
			PaniniModuleTask previous = _headNode;
			while(current!=t){
				previous = current;
				current = current.next;
			}
			if(previous == current)
				_headNode =null;
			else	
				previous.next = current.next;
		}
		
		private PaniniModuleTask _headNode = null; 
		public void run() {
			// Implementation relies upon at least one module being present 
			PaniniModuleTask current = _headNode;
			while(true){
				if(current.size!=0){
					if(current.run() == true)
						remove(this, current);
					if(_headNode == null)
						break;
				}
				synchronized(this) {
					current = current.next; 
				}
			}
		}
		
		private static PaniniTaskPool[] _getInstance = new PaniniTaskPool[1]; 
		private PaniniTaskPool(){}	
		private static int poolSize = 1;
		private static int nextPool = 0;;
}
