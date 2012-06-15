
library bool {
    interface BooleanI {
        public boolean value();
    }

    class BooleanC implements BooleanI {
        private boolean v;

        public BooleanC(boolean v) { this.v = v; }
    
        public boolean value() { return v; }
    }
}

module Fork () {
    include bool;

    boolean isTaken = false;
    
    BooleanC take() {
        if (isTaken) return new BooleanC(false);
        else {
            isTaken = true; return new BooleanC(true);
        }
    }

    void giveBack() { 
        isTaken = false;
    }
}


module Philosopher (Fork left, Fork right, String name) {
    include bool;

    void run() {
        while (true) {
            think();
            tryEat();
        }
    }

    void think() {
    	synchronized (System.out) {
        System.out.println(name + " is thinking");}
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) { e.printStackTrace(); }
    }

    void tryEat() {
    	synchronized (System.out) {
            System.out.println(name + " is hungry");}
        synchronized (System.out) {
            System.out.println(name + " trying to take fork 1");}
        while (!left.take().value()) {}
        synchronized (System.out) {
            System.out.println(name + " trying to take fork 2");}
        while (!right.take().value()) {}
        synchronized (System.out) {
            System.out.println(name + " is eating");}

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) { e.printStackTrace(); }

        left.giveBack(); right.giveBack();
    }
}

system Philosophers {
    Fork f1, f2, f3; Philosopher p1, p2, p3;

    p1(f1,f2, "Aristotle");
    p2(f2,f3, "Demosthenes");
    p3(f3,f1, "Socrates");
}