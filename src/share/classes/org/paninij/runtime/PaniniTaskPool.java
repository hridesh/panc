package org.paninij.runtime;

final class PaniniTaskPool extends Thread {
		static final synchronized PaniniTaskPool add(PaniniModuleTask t) {
			// TODO: see load balancing
			if (!_getInstance.isAlive()) {
				_getInstance.start();
			}
			_getInstance._add(t);
			return _getInstance;
		}
		static final synchronized void remove(PaniniTaskPool pool, PaniniModuleTask t) {
			// TODO: if last module, stop the execution of _getInstance.
			pool._remove(t);
		}
		
		private final synchronized void _add(PaniniModuleTask t){
			// TODO: add to circular list.
		}
		private final synchronized void _remove(PaniniModuleTask t){
			// TODO: remove from circular list.
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
