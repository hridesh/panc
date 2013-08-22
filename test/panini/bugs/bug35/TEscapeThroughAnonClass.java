interface Escape {
  public void method();
}

capsule B {
  void escape(Escape fun){
    fun.method();
  }
}

capsule A {
  design{
    B b;
    A escapee;
  }

  void run() {
    b.escape(new Escape(){
      public void method(){
        //a reference to a capsule of type A
        //leaked to B
        escapee.nothing();
      }
    });
  }

  void nothing(){};

}

