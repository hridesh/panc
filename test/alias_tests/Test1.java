module M () {
    class A { int a = 1; }
    
    void run() {
        test();
    }

    // no effects because local
    void test() {
        A a = new A();
        a.a = 2;
    }
}

system Test1 {
    M m;
}