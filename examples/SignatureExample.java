signature Sig {
 void sigMethod(); 
}

capsule Mod () implements Sig {
 void sigMethod() {
  System.out.println("Hello World, Signature Style!");
 }
}

capsule Client (Sig s) {
 void run () {
  s.sigMethod(); 
 }
}

system SignatureExample {
 Mod m; Client c;
 c(m);
}
