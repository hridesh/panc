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
 * Contributor(s):
 */
package org.paninij.parser;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.parser.Tokens.Token;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.util.Assert;

/**
 * @author lorand
 * @since panini-0.9.2
 */
final public class PaniniTokens {
    private PaniniTokens() {
    };

    public static final String SYSLANG_WIRE_ALL = "wireall";
    public static final String SYSLANG_STAR = "star";
    public static final String SYSLANG_RING = "ring";
    public static final String SYSLANG_ASSOCIATE = "associate";

    private static final String[] wiringTokens = { SYSLANG_WIRE_ALL,
            SYSLANG_STAR, SYSLANG_RING, SYSLANG_ASSOCIATE };

    public static final String CAP_KIND_TASK = "task";
    public static final String CAP_KIND_MONITOR = "monitor";
    public static final String CAP_KIND_SEQUENTIAL = "sequential";

    private static final String[] capsuleKinds = {
        CAP_KIND_TASK, CAP_KIND_MONITOR, CAP_KIND_SEQUENTIAL
    };

    public static boolean isWiringToken(Token kind) {
        if (kind.kind != TokenKind.IDENTIFIER)
            return false;

        String tokenName = kind.name().toString();
        for (String s : wiringTokens) {
            if (tokenName.equals(s))
                return true;
        }
        return false;
    }

    public static boolean isSameKind(Token kind, String paniniToken) {
        if (kind.kind != TokenKind.IDENTIFIER)
            return false;

        return kind.name().toString().equals(paniniToken);
    }

    /**
     * Check if the token represents one of the concurrency modifiers for
     * Panini capsule declarations.
     * @param token
     * @return
     */
    public static boolean isCapsuleKindModifier(Token token) {
        if (token.kind != TokenKind.IDENTIFIER)
            return false;

        final String tokenName = token.name().toString();
        for (String conMod : capsuleKinds) {
            if (conMod.equals(tokenName))
                return true;
        }

        return false;
    }

    /**
     * Convert a token to it's modifier flag. If the token is not a modifier
     * flag, throw an exception.
     *
     * @param token
     * @return
     */
    public static long toModfier(Token token) {
        String s = token.name().toString();

        if (CAP_KIND_SEQUENTIAL.equals(s)) {
            return Flags.SERIAL;
        } else if (CAP_KIND_TASK.equals(s)) {
            return Flags.TASK;
        } else if (CAP_KIND_MONITOR.equals(s)) {
            return Flags.MONITOR;
        } else {
            Assert.error(s + " is not a known capsule concurrency modifier!");
            return 0;
        }
    }
}
