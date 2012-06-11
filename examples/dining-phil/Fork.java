module Fork () {
    boolean isTaken = false;

    boolean take() {
        if (isTaken) return false;
        else {
            isTaken = true; return true;
        }
    }

    void giveBack() { 
        isTaken = false;
    }
}
