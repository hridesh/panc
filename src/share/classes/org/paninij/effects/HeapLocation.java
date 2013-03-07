package org.paninij.effects;

public abstract class HeapLocation {
    private int id;
    private static int counter = 0;
    public HeapLocation() { id = counter++; }

    public boolean equals(Object o) {
        if (this == o) return true;
        else if (o instanceof HeapLocation) {
            return id==((HeapLocation)o).id;
        } else return false;
    }
    public int hashCode() {
        return id;
    }

    public HeapLocation union(HeapLocation l) {
        if (equals(l)) return this;
        else return null;
    }
}



