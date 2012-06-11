module WaitingRoom(int queue, int cap) {

    boolean sit(){
        if (queue<cap) {
            queue++;
          System.out.println("Sitting in waiting room");
            return true;
        }
        else
            return false;
    }

    boolean leave(){
        if(queue>0){
            queue--;
            return true;
        }
        else
            return false;
    }
}
