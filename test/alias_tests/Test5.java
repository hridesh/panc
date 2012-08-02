module M () {
    class A { 
        int a = 1;
        
        // writes to this.2;
        void test() {
            A a = this;
            a.a = 2;
        }
    }
    A a = new A();
    
    void run() {
        a.test();
    }
}

system Test5 {
    M m;
}