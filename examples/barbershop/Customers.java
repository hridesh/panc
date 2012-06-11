module Customers(Barber b, WaitingRoom r) {
    void run() {
        hairCut();
    }

    void hairCut(){
        System.out.println("Customer wants haircut");
        if(!b.isSleeping()){
            if(!r.sit()) {
                System.out.println("Waiting room is full, so leaving");
                leave();
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) { e.printStackTrace(); }
                hairCut();
            }
        }
        else{
            b.wake();//b.wake(this);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { e.printStackTrace(); }
            hairCut();
        }               
    }

    void leave(){
      try {
          Thread.sleep(1000);
      } catch (InterruptedException e) { e.printStackTrace(); }
        hairCut();
    }
}
