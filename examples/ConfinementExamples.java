
class TestC {
	TestC next;
	void setNext(TestC next) { this.next = next; }
}

capsule C {
	void test(TestC tc) {  }
}

capsule M (C c) {
	TestC tc = new TestC();
	void mtest() {
		tc.setNext(tc);
		c.test(tc);
	}

	TestC mtest2() {
		return tc;
	}
}

capsule ConfineTest {
    =>= {
        C c; M m;
        m(c);
    }
}
