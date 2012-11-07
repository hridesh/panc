signature Sig {
 void sigMethod(); 
}

module Mod () implements Sig {
 void sigMethod() {
  System.out.println("Hello World, Signature Style!");
 }
}

module Client (Sig s) {
 void run () {
  s.sigMethod(); 
 }
}

system SignatureExample {
 Mod m; Client c;
 c(m);
}
