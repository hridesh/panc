module M () {
    class A { int a = 1; }
    A a = new A();
    
    void run() {
        test();
    }

    // has an effect because local variable a aliases module field
    void test() {
        A a1 = a;
        a1.a = 2;
    }
}

system Test3 {
    M m;
}