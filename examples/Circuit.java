signature Component {
    void pin1(); 
}
signature BinaryComponent {
    void pin1(); 
    void pin2();
}
capsule And (Component o) implements BinaryComponent {
    boolean _pin1 = false; 
    boolean _pin2 = false;
    void pin1() {
        _pin1 = true; 
        trigger();
    }
    void pin2() {
        _pin2 = true;
        trigger(); 
    }
    private void trigger() {
        if(_pin1 && _pin2) {
            _pin1 = _pin2 = false; 
            o.pin1();
        }
    }
}
capsule LED implements Component {
    boolean _state = false;
    void pin1 () {
        _state = !_state;
        if(_state) System.out.println("LED on");
        else System.out.println("LED off");
    }
}
capsule Circuit {
    design {
        And and;
        LED led; 
        and(led);
    }
    void run() {
        and.pin1();
        and.pin2();
    }
}
