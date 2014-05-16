/**
 * Leader election example.
 *
 * @author Sean L. Mooney
 */
package seqconstex.leader;

class Msg {
    static final int FIRST  = 0;
    static final int SECOND = 1;

    final int type;
    final int id;
    public Msg(int type, int id) {
        this.type = type;
        this.id = id;
    }

    public String toString() {
        return typeAsString() + ": " + id;
    }

    private String typeAsString() {
        switch(type) {
        case FIRST : return "FIRST";
        case SECOND: return "SECOND";
        default: return "UNKNOWN";
        }
    }
}

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
        right.elect(new Msg(Msg.FIRST, id));
    }

    private void incCounter() {
        System.out.println(id + " herpderp");
    }

    void elect(Msg msg) {
        System.out.println(id + " Elect a leader\n\tGot: " + msg.toString());

        switch(msg.type) {
            case Msg.FIRST:
                number = msg.id;
                if(active && number != maxi) {
                  right.elect(new Msg(Msg.SECOND,number));
                  neighborR = number;
                } else if(!active) {
                  System.out.println(id + " is not yet active, pass " + number + " to the right.");
                  right.elect(new Msg(msg.FIRST,number));
                }
                break;
            case Msg.SECOND:
                number = msg.id;
                if(active) {
                  if(neighborR > number && neighborR > maxi) {
                    maxi = neighborR;
                    right.elect(new Msg(Msg.FIRST,neighborR));
                  } else {
                    active = false;
                    incCounter();
                  }
                } else {
                  right.elect(new Msg(Msg.SECOND,number));
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown message type: " + msg.type);
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

        ps[0].elect(new Msg(Msg.FIRST, ps[0].getLPId().value));
    }
}