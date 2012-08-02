module M () {
    class A { int a = 1; }
    A a = new A();
    
    void run() {
        test();
    }

    // has an effect because local variable a aliases module field
    void test() {
        A a1 = new A();
        setA(a1);
        a1.a = 2;
    }

    private void setA(A newA) { a = newA; }
}

system Test4 {
    M m;
}