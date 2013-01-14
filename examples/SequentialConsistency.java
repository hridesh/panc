class IntegerC {
        int value; public IntegerC(int value) { this.value = value; }
        public int value() { return value; }
    }

capsule Main (Indirection i1, Indirection i2) {

    void doSomething(int i) {
        if(i<12) i1.set(new IntegerC(1));
        else i2.set(new IntegerC(2));
        i1.get().value();
    }
}

capsule Store () {
    int state = 0;

    void set(IntegerC newState) {
        state = newState.value();
    }
    IntegerC get() {
        return new IntegerC(state);
    }
}

capsule Indirection (Store s) {

    void set(IntegerC newStore) {
        yield((long)(Math.random()*1000));
        s.set(newStore);
    }
    IntegerC get() {
        yield((long)(Math.random()*1000));
        return s.get();
    }
}

system SequentialConsistency {
    Main m; Store s; Indirection i1, i2;
    i1(s); i2(s); m(i1, i2);    
}