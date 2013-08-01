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

/* @test
 * @summary
 * @build PancDesignDeclTest
 * @run main PancDesignDeclTest
 */

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.AbstractElementVisitor7;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

/**
 * A quick test put together to test capsule design decl translation.
 *
 * This test is in some need of refactoring, and could be made much more general
 * to accomodate making assertions about many of the translation, typechecking,
 * system graph, etc. elements of the system, which need more than a simple
 * 'does/doesn't' compile test.
 *
 * @author Sean L. Mooney
 * @since panini-0.9.2
 */
public class PancDesignDeclTest {

    final JavaCompiler tool;

    public PancDesignDeclTest() {
        tool = ToolProvider.getSystemJavaCompiler();
        System.out.println("java.home=" + System.getProperty("java.home"));

    }

    public void test() throws IOException {

        JavacTaskImpl tasks = getTasks("capsule M { design { " + " D d; C c;"
                + "}}" + "capsule D { void run() {} }"
                + "capsule C { void run(){} }");

        Iterable<? extends Element> enters = tasks.analyze();
        Context context = tasks.getContext();
        Names names = Names.instance(context);

        List<Name> capsules = new ArrayList<Name>(2);

        capsules.add(names.fromString("d"));
        capsules.add(names.fromString("c"));

        final Name reqTypeName = names.fromString("M$thread");
        for (Element e : enters) {
            if (e.getKind() == ElementKind.CLASS
                    && e.getSimpleName().equals(reqTypeName)) {
                testDeclTranslation(context, e, capsules);
            }
        }
    }

    /**
     * Test design decl translatation. Assert that EACH name with a capsule type
     * (provide a priori) gets exactly 1 start statement, nothing gets a 'run'
     * statement TODO: Make assertions about whether or not a decl gets a
     * ref$count and what the ref count should be.
     */
    public void testDeclTranslation(final Context context, final Element t,
            final Collection<Name> expectations) {
        final HashMap<Name, Integer> startCounts = new HashMap<Name, Integer>();
        final TreeScanner<Void, Void> scanner = new TreeScanner<Void, Void>() {

            private boolean visitingMethodInvocation;
            Names names = Names.instance(context);

            void checkStartCount(Name n) {
                if (startCounts.containsKey(n)) {
                    Integer c = startCounts.get(n);
                    if (c > 0) {
                        throw new AssertionError(n
                                + " has more than 1 start call");
                    }
                } else {
                    startCounts.put(n, 1);
                }
            }

            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
                try {
                    visitingMethodInvocation = true;
                    node.getMethodSelect().accept(this, p);
                } finally {
                    visitingMethodInvocation = false;
                }
                return null;
            }

            @Override
            public Void visitMemberSelect(MemberSelectTree node, Void p) {
                if (visitingMethodInvocation) {
                    ExpressionTree lhs = node.getExpression();
                    if (lhs instanceof JCIdent) {
                        Name selected = ((JCIdent) lhs).name;
                        Name ident = node.getIdentifier();
                        if (expectations.contains(selected)) {
                            if (ident.equals(names.panini.Start)) {
                                checkStartCount(selected);
                            } else if (ident.equals(names.panini.Run)) {
                                throw new AssertionError(
                                        "Found illegal run invocation on a capusle type.");
                            }
                        }
                    }
                } else {
                    super.visitMemberSelect(node, p);
                }

                return null;
            }
        };

        t.accept(new AbstractElementVisitor7<Boolean, Void>() {
            final Names names = Names.instance(context);

            @Override
            public Boolean visitPackage(PackageElement e, Void p) {
                return true;
            }

            @Override
            public Boolean visitType(TypeElement e, Void p) {
                List<? extends Element> enclosedElements = e
                        .getEnclosedElements();
                for (Element i : enclosedElements) {
                    i.accept(this, null);
                }
                return true;
            }

            @Override
            public Boolean visitVariable(VariableElement e, Void p) {
                return true;
            }

            @Override
            public Boolean visitExecutable(ExecutableElement e, Void p) {
                boolean result = true;
                Name n = e.getSimpleName();
                switch (e.getKind()) {

                case METHOD:
                    if (n.equals(names.panini.InternalCapsuleWiring)) {
                        result &= checkDesignDecl(e);
                    }
                    break;
                default:
                    break;
                }
                return result;
            }

            private boolean checkDesignDecl(ExecutableElement e) {
                if (e instanceof MethodSymbol) {
                    MethodSymbol ws = (MethodSymbol) e;
                    scanner.scan(ws.tree, null);
                    for (Name n : expectations) {
                        Integer c = startCounts.get(n);
                        if (c == null) {
                            throw new AssertionError("Capsule " + n
                                    + " not started");
                        } else if (c != 1) {
                            throw new AssertionError("Capsule " + n
                                    + " started " + c
                                    + " times. Must start only once.");
                        }
                    }
                    return true;
                }
                return false;
            }

            @Override
            public Boolean visitTypeParameter(TypeParameterElement e, Void p) {
                return true;
            }
        }, null);
    }

    static class MyFileObject extends SimpleJavaFileObject {

        private String text;

        public MyFileObject(String text) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }

    public JavacTaskImpl getTasks(String code) {
        return (JavacTaskImpl) tool.getTask(null, null, null, null, null,
                Arrays.asList(new MyFileObject(code)));
    }

    public CompilationUnitTree getCompilationUnitTree(String code)
            throws IOException {
        JavacTaskImpl ct = getTasks(code);
        CompilationUnitTree cut = ct.parse().iterator().next();
        return cut;
    }

    public static void main(String[] args) throws IOException {
        new PancDesignDeclTest().test();
    }
}
