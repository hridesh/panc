class MyBool{
	boolean bool;
	public MyBool(boolean b){
		bool = b;
	}
	boolean getValue(){
		return bool;
	}
}

capsule FittingRoom(){
	boolean inUse = false;
	MyBool request(boolean go, boolean next){
		if(inUse || !go){
			return door.wait(go, next);
		}
		else{
			inUse = true;
			return new MyBool(true);
		}
	}
	
	void leave(){
		inUse = false;
		door.go();
	}
	
	design{
		WaitingQueue door;
		Person p1,p2,p3;
		p1("Sam", this);
		p2("Sally", this);
		p3("Andy", this);
		door(this);
	}
}

capsule WaitingQueue(FittingRoom f){
	boolean canGo=true;
	boolean hasNext=false;
	boolean first = true;
	MyBool wait(boolean go, boolean next){
		yield(10);
		if(first|| (canGo && next)){
			first = false;
			hasNext = false;
			canGo = false;
			return f.request(true, true);
		} else if(hasNext == false){
			hasNext = true;
			return f.request(false, true);
		} else
			return f.request(false, next);
	}
	
	void go(){
		canGo = true;
	}
}

capsule Person(String pname, FittingRoom f){
	void run(){
		while(true){
			yield(4000);
			report(" is requesting the fitting room.");
			if(f.request(false, false).getValue() == true);
			report(" has entered the fitting room.");
			yield(1000);
			f.leave();
			report(" has left the fitting room.");
		}
	}
	
	void report(String message){
		System.out.println(":"+ pname + message);
	}
}
