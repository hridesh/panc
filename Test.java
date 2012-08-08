module M (M2 m2) {
    boolean stuff = false;
    boolean isSleeping = false;

    static class A {
        public static void asdf() {}
    }

    void doSomething() {
        boolean localVar = false;
        doSomething();
        A.asdf();
        m2.doSomethingElse();
        isSleeping = stuff;
        localVar = isSleeping;
    }
}

module M2 () {
    void doSomethingElse() {}
}

system Test {
    M m;
    M2 m2;
    m(m2);
}