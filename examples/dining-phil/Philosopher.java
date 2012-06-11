module Philosopher (Fork left, Fork right, String name) {

    void run() {
        while (true) {
            think();
            tryEat();
        }
    }

    void think() {
        System.out.println(name + " is thinking");
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) { e.printStackTrace(); }
    }

    void tryEat() {
        System.out.println(name + " is hungry");

        System.out.println(name + " trying to take fork 1");
        while (!left.take()) {}
        System.out.println(name + " trying to take fork 2");
        while (!right.take()) {}

        System.out.println(name + " is eating");

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) { e.printStackTrace(); }

        left.giveBack(); right.giveBack();
    }
}
