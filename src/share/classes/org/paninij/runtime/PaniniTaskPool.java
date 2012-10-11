package org.paninij.runtime;

final class PaniniTaskPool extends Thread {
		static final synchronized PaniniTaskPool add(PaniniModuleTask t) {
			// TODO: see load balancing
			_getInstance._add(t);
			if (!_getInstance.isAlive()) {
				_getInstance.start();
			}
			return _getInstance;
		}
		static final synchronized void remove(PaniniTaskPool pool, PaniniModuleTask t) {
			// TODO: if last module, stop the execution of _getInstance.
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
				current.run();
				synchronized(this) {
					current = current.next; 
				}
			}
		}
		
		private static final PaniniTaskPool _getInstance = new PaniniTaskPool(); 
		private PaniniTaskPool(){}			
}
