capsule M (M2 m2) {
    boolean stuff = false;
    boolean isSleeping = false;

    class A {
        int i = 0;
        public void asdf() {
            i = 1;
            doSomething();
        }
    }

    void doSomething() {
        boolean localVar = false;
        this.doSomething();
        new A().asdf();
        m2.doSomethingElse();
        isSleeping = stuff;
        localVar = isSleeping;
    }
}

capsule M2 () {
    int j = 5;
    void doSomethingElse() {
        int i = 0;
        j = 6;
    }
}

system Test {
    M m;
    M2 m2;
    m(m2);
}