
library boolint {
    class BooleanC {
        boolean v;
        public BooleanC(boolean v) { this.v = v; }
        public boolean value() { return v; }
    }

    class IntC {
        int v;
        public IntC(int v) { this.v = v; }
        public int value() { return v; }
    }
}


library customer {
    class Customer {
        private int id;
        public Customer(int id) { this.id = id; }
        public int getID() { return id; }
    }
}
  
module Barber(WaitingRoom r, boolean isSleeping) {
    include customer;
    include boolint;
          
    void wake(Customer c){
        isSleeping = false;
        System.out.println("Barber Woke up");
        work(c);
    }
          
    void sleep(){
        System.out.println("Barber went to sleep");
        isSleeping = true;
    }
          
    void work(Customer c){
        System.out.println("Barber working on customer " + c.getID());  
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) { e.printStackTrace(); }
        checkRoom();
    }
          
    void checkRoom(){
        Customer c = r.whosNext();
        if(c!=null) work(c);
        else sleep();
    }
          
    BooleanC isSleeping(){
        return new BooleanC(isSleeping);
    }
          
}
  
module WaitingRoom(int cap) {
    include java.util.Queue;
    include java.util.LinkedList;
    include boolint;
  
    Queue<Customer> queue = new LinkedList<Customer>();
    int idCounter = 0;
  
    IntC incIdCount() { return new IntC(idCounter++); }
  
    BooleanC sit(Customer c){
        if (queue.size()<cap) {
            queue.offer(c);
            System.out.println("Customer " + c.getID() + " Sitting in waiting room");
            return new BooleanC(true);
        }
        else
            return new BooleanC(false);
    }
          
    Customer whosNext() {
        return queue.poll();
    }
}
  
module Customers(Barber b, WaitingRoom r) {
    include customer;
  
    int idCounter = 0;
  
    private void newCustomer() {
        hairCut(new Customer(r.incIdCount().value()));
    }
  
    void run() {
        newCustomer();
    }
  
    private void hairCut(Customer c){
        System.out.println("Customer " + c.getID() + " wants haircut");
        if(!b.isSleeping().value()){
            if(!r.sit(c).value()) {
                System.out.println("Waiting room is full, so " + c.getID() + " leaving");
                leave();
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) { e.printStackTrace(); }
                newCustomer();
            }
        }
        else{
            b.wake(c);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { e.printStackTrace(); }
            newCustomer();
        }               
    }
          
    private void leave(){
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) { e.printStackTrace(); }
        newCustomer();
    }
}
  
system Barbershop2 {
    Barber b;
    WaitingRoom w;
    Customers c[5];
  
    b(w, true);
    w(10);       

    for(Customers cs : c){
        cs(b, w);
    }
}
