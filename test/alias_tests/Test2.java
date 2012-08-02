module M () {
    class A { int a = 1; }
    
    void run() {
        test();
    }

    // no effects because local
    void test() {
        A a1 = new A();
        A a2 = a1;
        a2.a = 2;
    }
}

system Test2 {
    M m;
}