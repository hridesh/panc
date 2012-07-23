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


abstract class Effect {}
class EmptyEffect extends Effect {}
class FieldReadEffect extends Effect {
    VarSymbol field; public FieldReadEffect(VarSymbol field) { this.field = field; }
}
class FieldWriteEffect extends Effect {
    VarSymbol field; public FieldWriteEffect(VarSymbol field) { this.field = field; }
}
class OpenEffect extends Effect {
    MethodSymbol method; EffectSet otherEffects; 
    public OpenEffect(MethodSymbol method) { this.method = method; this.otherEffects = new EffectSet(); }
}
class MethodEffect extends Effect {
    MethodSymbol method; 
    public MethodEffect(MethodSymbol method) { this.method = method; }
}
class BottomEffect extends Effect {}


public class EffectSet extends HashSet<Effect> {
    public boolean add(Effect e) {
        // if (e instanceof OpenEffect) {
        //     OpenEffect oe = (OpenEffect) e;
        //     openEffects.add(oe);
        // }
        return super.add(e);
    }

    boolean intersects(EffectSet es) {
        for (Effect e : es) {
            if ((contains(e) && !(e instanceof EmptyEffect)) || e instanceof BottomEffect) return true;
        }
        return false;
    }

    public boolean doesInterfere(EffectSet before, EffectSet after) { return true; }
    static final long serialVersionUID = 42L;
}