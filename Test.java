module M () {
    boolean stuff = false;
    boolean isSleeping = false;

    class A {
        public static void asdf() {}
    }

    void doSomething() {
        boolean localVar = false;
        doSomething();
        A.asdf();
        isSleeping = stuff;
        localVar = isSleeping;
    }
}

system Test {
    M m;
}