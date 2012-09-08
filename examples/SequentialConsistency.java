//library integerc {
class IntegerC {
        int value; public IntegerC(int value) { this.value = value; }
        public int value() { return value; }
    }
//}

module Main (Indirection i1, Indirection i2) {
//    include integerc;

    void doSomething(int i) {
        if(i<12) i1.set(new IntegerC(1));
        else i2.set(new IntegerC(2));
        i1.get().value();
    }
}

module Store () {
//    include integerc;

    int state = 0;

    void set(IntegerC newState) {
        state = newState.value();
    }
    IntegerC get() {
        return new IntegerC(state);
    }
}

module Indirection (Store s) {
//    include integerc;

    void set(IntegerC newStore) {
//        yield((long)(Math.random()*1000));
        s.set(newStore);
    }
    IntegerC get() {
//        yield((long)(Math.random()*1000));
        return s.get();
    }
}

system SequentialConsistency {
    Main m; Store s; Indirection i1, i2;
    i1(s); i2(s); m(i1, i2);    
}