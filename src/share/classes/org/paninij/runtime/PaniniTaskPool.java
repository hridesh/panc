package org.paninij.runtime;

final class PaniniTaskPool extends Thread {
		
		private static boolean initiated = false;
		static final synchronized void init(int size) throws Exception{
			if(initiated)
				throw new Exception("Target already initialized");
			poolSize = size;
			_getInstance = new PaniniTaskPool[size];
			for(int i=0;i<_getInstance.length;i++){
				_getInstance[i] = new PaniniTaskPool();
			}
			initiated = true;
		}
		
		static final synchronized PaniniTaskPool add(PaniniCapsuleTask t) {
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
		static final synchronized void remove(PaniniTaskPool pool, PaniniCapsuleTask t) {
			pool._remove(t);
		}
		
		private final synchronized void _add(PaniniCapsuleTask t){
			if(_headNode==null){
				_headNode = t;
				t.panini$capsule$next = t;
			}else{
				t.panini$capsule$next = _headNode.panini$capsule$next;
				_headNode.panini$capsule$next = t;
			}
			t.panini$capsule$init();
		}
		
		private final synchronized void _remove(PaniniCapsuleTask t){
			PaniniCapsuleTask current = _headNode;
			PaniniCapsuleTask previous = _headNode;
			while(current!=t){
				previous = current;
				current = current.panini$capsule$next;
			}
			if(previous == current) {
				if (current.panini$capsule$next == current) {
					_headNode = null;
					return;
				}
				PaniniCapsuleTask tmp = previous;
				while(tmp != previous.panini$capsule$next)	
					previous = previous.panini$capsule$next;
				_headNode = current.panini$capsule$next;
				previous.panini$capsule$next = _headNode;
			} else	
				previous.panini$capsule$next = current.panini$capsule$next;
		}
		
		private PaniniCapsuleTask _headNode = null; 
		public void run() {
			// Implementation relies upon at least one capsule being present 
			PaniniCapsuleTask current = _headNode;
			while(true){
				if(current.panini$capsule$size!=0){
					if(current.run() == true)
						remove(this, current);
					if(_headNode == null)
						break;
				}
				synchronized(this) {
					current = current.panini$capsule$next; 
				}
			}
		}
		
		private static PaniniTaskPool[] _getInstance = new PaniniTaskPool[1]; 
		private PaniniTaskPool(){}	
		private static int poolSize = 1;
		private static int nextPool = 0;;
}
