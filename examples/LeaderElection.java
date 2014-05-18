/**
 * Leader election example.
 *
 * @author Sean L. Mooney
 */

class Int{
    public final int value;
    public Int(int v) { this.value = v; }
}

capsule LProcess(LProcess right, int id)  {
    int number = 0, maxi = 0, neighborR = 0;
    boolean active = false;

    void init() {
        System.out.println("Initializing LProcess " + id);
        maxi = id;
        active = true;
        System.out.println(id + " is now active.");
        right.circulate(id);
    }
    
    void circulate(int id) {
        if(active && id != maxi) {
          right.elect(id);
          neighborR = id;
        } else if(!active) {
          System.out.println(id + " is not yet active, pass " + id + " to the right.");
          right.circulate(id);
        }
    }
    
    void elect (int id) {
        if(active) {
          if(neighborR > id && neighborR > maxi) {
            maxi = neighborR;
            right.circulate(neighborR);
          } else {
            active = false;
          }
        } else {
          right.elect(id);
        }
    }

    Int getLPId() { return new Int(id); }
}

capsule Election {
	
	design {
		LProcess ps[4]; 
	    ps[0](ps[1], 0);
	    ps[1](ps[2], 1);
	    ps[2](ps[3], 2);
	    ps[3](ps[0], 3);
	}
	
    void run() {
        for(LProcess p : ps){
            p.init();
        }

        ps[0].circulate(ps[0].getLPId().value);
    }
}