/*
 * This file is part of the Panini project at Iowa State University.
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 * 
 * For more details and the latest version of this code please see
 * http://paninij.org
 * 
 * Contributor(s): Rex Fernando
 */

package org.paninij.effects;

import java.util.HashMap;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Symbol;


public class HeapRepresentation {
    private HashMap<Symbol, HeapLocation> locations = new HashMap<Symbol, HeapLocation>();
    
    public HeapLocation locationForSymbol(Symbol s) {
        HeapLocation l = locations.get(s);
        return l!=null ? l : UnknownHeapLocation.instance;
    }
    
    public HeapRepresentation union(HeapRepresentation h) {
        HeapRepresentation result = new HeapRepresentation();

        for (Symbol s : locations.keySet()) {
            HeapLocation l1 = locationForSymbol(s);
            HeapLocation l2 = h.locationForSymbol(s);
            
            if (l2 != null) {
                result.locations.put(s, l1.union(l2));
            } else result.locations.put(s, l1);
        }
        return result;
    }

    public String toString() {
        return locations.toString();
    }

    public void add(Symbol s, HeapLocation l) { locations.put(s, l); }

    public boolean equals(Object o) {
        if (o instanceof HeapRepresentation) {
            HeapRepresentation hr = (HeapRepresentation)o;
            return locations.equals(hr.locations);
        }
        else return false;
    }
}