import java.util.Queue;
import java.util.LinkedList;

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


    class Customer {
        private int id;
        public Customer(int id) { this.id = id; }
        public int getID() { return id; }
    }

    class MaybeCustomer {
        private Customer c;
        public MaybeCustomer(Customer c) { this.c = c; }
        public Customer getCustomer() { return c; }
    }
  
capsule Barber(WaitingRoom r, boolean isSleeping) {
          
    void wake(Customer c){
        isSleeping = false;
        System.out.println("Barber Woke up");
        work(c);
        Customer n = r.whosNext().getCustomer();
        while (n!=null) {
            work(n);
            n = r.whosNext().getCustomer();
        }
        sleep();
        
    }
          
    void sleep(){
        System.out.println("Barber went to sleep");
        isSleeping = true;
    }
          
    void work(Customer c){
        System.out.println("Barber working on customer " + c.getID());  
        yield(1000);
    }
          
    BooleanC isSleeping(){
        return new BooleanC(isSleeping);
    }
          
}
  
capsule WaitingRoom(int cap) {
  
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
          
    MaybeCustomer whosNext() {
        return new MaybeCustomer(queue.poll());
    }
}
  
capsule Customers(Barber b, WaitingRoom r) {
    int idCounter = 0;
  
    void run() {
        while (true) {
            Customer c = new Customer(r.incIdCount().value());
            System.out.println("Customer " + c.getID() + " wants haircut");
            if (!b.isSleeping().value()) {
                trySit(c);
            } else {
                System.out.println("Customer is waking barber up");
                b.wake(c);
            }
            yield(1000);
        }
    }
  
    void trySit(Customer c) {
        System.out.println("Barber is busy, trying to sit down");
        if(!r.sit(c).value()) {
            System.out.println("Waiting room is full, so leaving");
        }
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
