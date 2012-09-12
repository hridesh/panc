
//library bool {
    class BooleanC {
        boolean v;
        public BooleanC(boolean v) { this.v = v; }
        public boolean value() { return v; }
    }
//}

module Barber(WaitingRoom r, boolean isSleeping) {
//    include bool;
          
    void wake(){
        isSleeping = false;
        System.out.println("Barber Woke up");
        work();
        while (r.leave().value()) {
            work();
        }
        sleep();
    }
          
    void sleep(){
        System.out.println("Barber went to sleep");
        isSleeping = true;
    }
          
    void work(){
        System.out.println("Barber working");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) { e.printStackTrace(); }
    }
          
    BooleanC isSleeping(){
        return new BooleanC(isSleeping);
    }
          
}
  
module WaitingRoom(int queue, int cap) {
//    include bool;
  
    BooleanC sit(){
        if (queue<cap) {
            queue++;
            System.out.println("Sitting in waiting room");
            return new BooleanC(true);
        }
        else
            return new BooleanC(false);
    }
          
    BooleanC leave(){
        if(queue>0){
            queue--;
            return new BooleanC(true);
        }
        else
            return new BooleanC(false);
    }
}
  
module Customers(Barber b, WaitingRoom r) {
    void run() {
        while (true) {
            System.out.println("Customer wants haircut");
            if (!b.isSleeping().value()) {
                trySit();
            } else {
                System.out.println("Customer is waking barber up");
                b.wake();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { e.printStackTrace(); }
        }
          
    }
  
    void trySit(){
        System.out.println("Barber is busy, trying to sit down");
        if(!r.sit().value()) {
            System.out.println("Waiting room is full, so leaving");
        }
    }
       
}
  
system Barbershop {
    Barber b;
    WaitingRoom w;
    Customers cs[5];
  
    b(w, true);
    w(0, 10);       
    for(Customers c : cs){
        c(b, w);
    }
}