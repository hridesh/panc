
import java.io.IOException;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.InternalWiringMethod;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

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
 * Contributor(s): Sean L. Mooney
 */

/* @test
 * @summary Test to ensure the capsule kind modifiers are repect in design blocks.
 * @run main TCapsuleKinds
 */

public class TCapsuleKinds extends util.AbstractPancTest {

    static final String testCapsule =
                    "capsule C () {" +
                    "    design {" +
                    "        sequential C cSeq;" +
                    "        monitor C cMon;" +
                    "        C cThd;" +
                    "        task C cTask;" +
                    "    }" +
                    "}";

    private void testCapsuleKinds() throws IOException {
        JavacTaskImpl tasks = getTasks(testCapsule);

        Iterable<? extends CompilationUnitTree> parsed = tasks.parse();
        final Context context = tasks.getContext();
        final Names names = Names.instance(context);

        class ExpectedFlag {
            final Name name;
            final long flag;
            ExpectedFlag(String n, long flag) {
                this.name = names.fromString(n);
                this.flag = flag;
            }
        }

        //Names and flags we expect.
        List<ExpectedFlag> expected = List.of(
                  new ExpectedFlag("cSeq", Flags.SERIAL)
                , new ExpectedFlag("cMon", Flags.MONITOR)
                , new ExpectedFlag("cThd", 0)
                , new ExpectedFlag("cTask", Flags.TASK)
        );

        //Assume there is 1 parsed item in the tree.
        List<VariableTree> decls =
                parsed.iterator().next().accept(new CapsuleKindScanner(), null);

        for(List<VariableTree> l = decls; l.nonEmpty(); l = l.tail, expected = expected.tail) {
            VariableTree varTree = l.head;
            assertEquals("Unexpected name.", expected.head.name, varTree.getName());
            JCVariableDecl varDecl = (JCVariableDecl)varTree;
            if(expected.head.flag != 0) {
                assertTrue(
                        "Expected to find a " + Flags.asFlagSet(expected.head.flag) +
                        " in " + varTree,
                        (varDecl.mods.flags & expected.head.flag) != 0);
            } else { //Special case for no flag on cThd;
                assertTrue(
                        "Did expected to find a " + Flags.asFlagSet(expected.head.flag) +
                        " in " + varTree,
                        varDecl.mods.flags == 0);
            }
        }

    }

    public static void main(String[] args) throws IOException {
        new TCapsuleKinds().testCapsuleKinds();
    }

    static class CapsuleKindScanner extends TreeScanner<List<VariableTree>, Void> {
        private boolean inDesignDecl = false;
        /* (non-Javadoc)
         * @see com.sun.source.util.TreeScanner#visitWiringBlock(com.sun.source.tree.InternalWiringMethod, java.lang.Object)
         */
        @Override
        public final List<VariableTree> visitWiringBlock(InternalWiringMethod node, Void p) {
            boolean prevInDesignDecl = inDesignDecl;
            try {
                inDesignDecl = true;
                return super.visitWiringBlock(node, p);
            } finally {
                inDesignDecl = prevInDesignDecl;
            }
        }

        /* (non-Javadoc)
         * @see com.sun.source.util.TreeScanner#visitVariable(com.sun.source.tree.VariableTree, java.lang.Object)
         */
        @Override
        public final List<VariableTree> visitVariable(VariableTree node, Void p) {
            if( inDesignDecl ) {
                return List.of(node);
            }
            return null;
        }

        /* (non-Javadoc)
         * @see com.sun.source.util.TreeScanner#reduce(java.lang.Object, java.lang.Object)
         */
        @Override
        public final List<VariableTree> reduce(List<VariableTree> r1,
                List<VariableTree> r2) {
            ListBuffer<VariableTree> lbuf = new ListBuffer<VariableTree>();
            if(r2 != null)
                lbuf.appendList(r2);
            if(r1 != null)
                lbuf.appendList(r1);
            return lbuf.toList();
        }
    }
}
