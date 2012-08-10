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

import java.util.HashSet;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Symbol;


abstract class Effect {}
class EmptyEffect extends Effect {}
class FieldReadEffect extends Effect {
    Symbol field; public FieldReadEffect(Symbol field) { this.field = field; }
    public String toString() { return "read " + field; }
    public boolean equals(Object o) {
        if (!(o instanceof FieldReadEffect)) return false;
        FieldReadEffect oe = (FieldReadEffect)o;
        return this.field == oe.field;
    }
    public int hashCode() { return field.hashCode(); }
}
class FieldWriteEffect extends Effect {
    Symbol field; public FieldWriteEffect(Symbol field) { this.field = field; }
    public String toString() { return "write " + field; }
    public boolean equals(Object o) {
        if (!(o instanceof FieldWriteEffect)) return false;
        FieldWriteEffect oe = (FieldWriteEffect)o;
        return this.field == oe.field;
    }
    public int hashCode() { return field.hashCode(); }
}
class OpenEffect extends Effect {
    MethodSymbol method; EffectSet otherEffects; 
    public OpenEffect(MethodSymbol method) { this.method = method; this.otherEffects = new EffectSet(); }
    public String toString() { return "open: " + method; }
    public boolean equals(Object o) {
        if (!(o instanceof OpenEffect)) return false;
        OpenEffect oe = (OpenEffect)o;
        return this.method == oe.method;
    }
    public int hashCode() { return method.hashCode(); }
}
class MethodEffect extends Effect {
    MethodSymbol method; 
    public MethodEffect(MethodSymbol method) { this.method = method; }
    public String toString() { return "method: " + method; }
    public boolean equals(Object o) {
        if (!(o instanceof MethodEffect)) return false;
        MethodEffect oe = (MethodEffect)o;
        return this.method == oe.method;
    }
    public int hashCode() { return method.hashCode(); }
}
class BottomEffect extends Effect {
    public String toString() { return "bottom"; }
        public boolean equals(Object o) {
        if (!(o instanceof BottomEffect)) return false;
        return true;
    }
    public int hashCode() { return 1; }
}


public class EffectSet extends HashSet<Effect> {
    ASTChain chain;

    public EffectSet() { super(); }
    public EffectSet(EffectSet e) { super(e); }

    boolean intersects(EffectSet es) {
        for (Effect e : es) {
            if ((contains(e) && !(e instanceof EmptyEffect)) || e instanceof BottomEffect) return true;
        }
        return false;
    }

    public boolean doesInterfere(EffectSet before, EffectSet after) { return true; }
    static final long serialVersionUID = 42L;
}