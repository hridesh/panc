module Barber(WaitingRoom r, boolean isSleeping) {

    void wake(){
        isSleeping = false;
      System.out.println("Barber Woke up");
        work();
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
        checkRoom();
    }

    void checkRoom(){
        if(r.leave()) work();
        else sleep();
    }

    boolean isSleeping(){
        return isSleeping;
    }

}
