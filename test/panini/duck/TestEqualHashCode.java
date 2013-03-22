class C {
 public int hashCode() {
  return 1;
 }
 public boolean equals(Object other) {
  return false;
 }
}

capsule Test {
 C testProc() {
  return new C(); 
 }
}

capsule Test2 (Test t) {
 void run() {
  t.testProc();
 }
}

system TestEqualHashCode {
 Test t;
}
