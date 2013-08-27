package org.paninij.consistency;

public class BiRoute {
    final Route r1;
    final Route r2;
  
    public BiRoute(Route r1, Route r2) {
        this.r1 = r1;
        this.r2 = r2;
    }
  
    public final int hashCode() {
        return r1.hashCode() + r2.hashCode();
    }
  
    public final boolean equals(Object obj) {
        if (obj instanceof BiRoute) {
            BiRoute other = (BiRoute)obj;
            return r1.equals(other.r1) && r2.equals(other.r2);
        }
        return false;
    }
}
