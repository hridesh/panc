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
 * Contributor(s): Sean L. Mooney, Lorand Szakacs
 */
package org.paninij.system;

import java.util.ArrayList;
import java.util.HashMap;

import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssociate;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCapsuleArray;
import com.sun.tools.javac.tree.JCTree.JCCapsuleArrayCall;
import com.sun.tools.javac.tree.JCTree.JCCapsuleDecl;
import com.sun.tools.javac.tree.JCTree.JCCapsuleWiring;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCTopology;
import com.sun.tools.javac.tree.JCTree.JCWireall;
import com.sun.tools.javac.tree.JCTree.JCProcInvocation;
import com.sun.tools.javac.tree.JCTree.JCRing;
import com.sun.tools.javac.tree.JCTree.JCStar;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSystemDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;

/**
 * Visit the system and interpret java computations to their values.
 * 
 * @author Sean L. Mooney, Lorand Szakacs
 * 
 */
public class SystemDeclRewriter extends TreeTranslator {

    InterpEnv<Name, JCTree> valueEnv;

    /**
     * this is a hack required for keeping track of array sizes.
     */
    InterpEnv<Name, JCVariableDecl> varDefToAstNodeEnv;
    final Log log;
    final TreeMaker make;
    final ArithTreeInterp atInterp;

    final TreeCopier<Void> copy;

    public JCSystemDecl rewrite(JCSystemDecl tree) {
        JCSystemDecl translated = super.translate(tree);
        translated.body.stats = unrollStatementsFromBodyStats(translated.body.stats);
        return translated;
    }

    /**
     * TODO: Refactor to removethis at some points in the future.
     *
     * This method flattens the statements. Unrolled topology statements are
     * implemented as JCStatements nodes that contain a list of statements.
     *
     * @param stats
     * @return
     */
    private List<JCStatement> unrollStatementsFromBodyStats(
            List<JCStatement> stats) {

        ArrayList<JCStatement> newStats = new ArrayList<JCStatement>(
                stats.size());

        //FIXME: refactor wireall to use JCUnrolledCrap
        for (JCStatement statement : stats) {
            if (statement.getKind() == Kind.EXPRESSION_STATEMENT) {
                JCExpressionStatement exprStatement = ((JCExpressionStatement) statement);
                final Kind exprKind = exprStatement.expr.getKind();
                //TOPOLOGY OPERATORS
                if (exprKind == Kind.WIREALL ||
                        exprKind == Kind.RING ||
                        exprKind == Kind.STAR_TOP ||
                        exprKind == Kind.ASSOCIATE) {
                    JCTopology wireall = (JCTopology) exprStatement.expr;
                    for (JCStatement unrolledStatement : wireall.unrolled) {
                        newStats.add(unrolledStatement);
                    }
                }  else
                    newStats.add(statement);
            } else if (statement.getKind() == null) {
                JCUnrolledStatement unrolledCrap = (JCUnrolledStatement)(JCTree)statement;
                for (JCStatement unrolledStatement : unrolledCrap.unrolled) {
                    newStats.add(unrolledStatement);
                }

            }else newStats.add(statement);
        }
        List<JCStatement> result = List.<JCStatement> from(newStats
                .toArray(new JCStatement[1]));
        return result;
    }

    public SystemDeclRewriter(TreeMaker treeMaker, Log log) {
        this.log = log;
        this.make = treeMaker;
        this.copy = new TreeCopier<Void>(make);
        this.atInterp = new ArithTreeInterp();
    }

    @Override
    public void visitIdent(JCIdent tree) {
        // FIXME: remove syso
        System.out.println("Visiting identifier: " + tree.name.toString());
        JCTree bound = valueEnv.lookup(tree.name);

        // Don't lose the identifier if we don't know about it!
        if (bound != null) {
            result = bound;
        } else {
            result = tree;
        }
    }

    @Override
    public void visitVarDef(JCVariableDecl tree) {

        // FIXME: remove syso
        System.out.println("Visiting var definition: " + tree.toString());
        if (tree.init != null) {
            tree.init = translate(tree.init);
        }
        valueEnv.bind(tree.name, tree.init);
        //FIXME
        varDefToAstNodeEnv.bind(tree.name, tree);

        result = tree;
    }

    @Override
    public void visitAssign(JCAssign tree) {

        final JCExpression translatedRHS = this.translate(tree.rhs);
       //in case of uninitialized variables
       //FIXME: might want to consider failing; artifact of not having attribution
        if (translatedRHS != null) {
            tree.rhs = translatedRHS;
        }

        //TODO: explore cases like x = y = 42;
        if (tree.lhs instanceof JCIdent) {
            Name assignTo = ((JCIdent) tree.lhs).name;
            valueEnv.bind(assignTo, tree.rhs);
        } else {
            // FIXME: Non-raw error message.
            log.rawError(tree.pos, tree
                    + " does not have an identifier on the left hand side!");
        }

        // FIXME: remove syso
        System.out.println("New assn: " + tree);
        //TODO return translatedRHS instead of tree;
        result = tree;
    }

    @Override
    public void visitBinary(final JCBinary tree) {
        JCTree lhs = translate(tree.lhs);
        JCTree rhs = translate(tree.rhs);

        if (lhs instanceof JCLiteral && rhs instanceof JCLiteral) {
            JCLiteral lhsLit = (JCLiteral) lhs;
            JCLiteral rhsLit = (JCLiteral) rhs;
            result = atInterp.interp(tree, lhsLit, rhsLit);
        } else {
            result = tree;
            log.rawError(tree.pos, "Trying to interpret " + tree
                    + " but one side is not a literal!");
        }

    }

    /**
     * This function will replace the JCManyToOne node with a
     * JCManyToOneUnrolled
     */
    //capsule C(A a, B b, int fortyTwo);
    //A capsuleArray[4];
    //wireall(capsuleArray, a, b, 42);
    //TODO: name suggestions: wireall
    @Override
    public void visitWireall(JCWireall tree) {
        // FIXME: remove syso
        System.out.println("Visiting wireall: " + tree.toString());
        super.visitWireall(tree);
        int capsuleArraySize = getCapsuleArraySize(tree);

        Name capsuleArrayName = getCapsuleArrayName(tree);
        JCStatement statements[] = new JCStatement[capsuleArraySize];
        for (int i = 0; i < capsuleArraySize; i++) {
            //TODO: refactor the name of make.CapsuleArrayCall -> CapsuleArrayWiring;
            statements[i] = make.CapsuleArrayCall(capsuleArrayName,
                    make.Literal(i), tree.many, tree.args);
        }

        System.out.println("Rewritten wireall statement: "
                + List.from(statements).toString());
        tree.unrolled = List.from(statements);
        result = tree;
    }

    @Override
    public void visitStar(JCStar tree){
    	// FIXME: remove syso
    	System.out.println("visiting star: " + tree.toString());
    	super.visitStar(tree);
    	JCExpression center = tree.center;
    	JCExpression others = tree.others;
    	List<JCExpression> args = tree.args;
    	int capsuleArraySize = getCapsuleArraySize(others);
    	ListBuffer<JCStatement> unrolledStats = new ListBuffer<JCStatement>();

    	for(int i = 0; i < capsuleArraySize; i++){
    		unrolledStats.add(make.Exec( //others -> center
    				make.WiringApply(make.Indexed(others, make.Literal(i)), args.prepend(center))
    				)
    		);
    	}
    	unrolledStats.add(make.Exec(
				make.WiringApply(center, args.prepend(others))
			)
	    );
    	System.out.println("Rewritten star statement: " + unrolledStats.toList().toString());
    	tree.unrolled = unrolledStats.toList();
    	result = tree;
    }

    @Override
    public void visitRing(JCRing tree){
    	// FIXME: remove syso
    	System.out.println("visiting ring: " + tree.toString());
    	super.visitRing(tree);
    	JCExpression capsules = tree.capsules;
    	List<JCExpression> args = tree.args;
    	int capsuleArraySize = getCapsuleArraySize(capsules);
    	ListBuffer<JCStatement> unrolledStats = new ListBuffer<JCStatement>();
    	for(int i = 0; i < capsuleArraySize-1; i++){
    	    JCCapsuleWiring wiring = make.WiringApply(make.Indexed(capsules, make.Literal(i)),
    							args.prepend(make.Indexed(capsules, make.Literal(i+1))));

    	    wiring.type = tree.type;
    		unrolledStats.add(make.Exec(wiring));
    	}
    	unrolledStats.add(make.Exec(
    				make.WiringApply(make.Indexed(capsules, make.Literal(capsuleArraySize-1)),
    						args.prepend(make.Indexed(capsules, make.Literal(0))))
    			)
    	);
    	System.out.println("Rewritten ring statement: "+ unrolledStats.toList().toString());
    	tree.unrolled = unrolledStats.toList();
    	result = tree;
    }

    @Override
    public void visitAssociate(JCAssociate tree){
    	// FIXME: remove syso
    	System.out.println("visiting associate: "+ tree.toString());
    	super.visitAssociate(tree);
    	JCExpression src = tree.src;
    	JCExpression dest = tree.dest;
    	List<JCExpression> args = tree.args;
    	if(tree.srcPos instanceof JCLiteral && tree.destPos instanceof JCLiteral
    			&& tree.len instanceof JCLiteral){
    		try{
    		int srcPos = ((Integer)((JCLiteral)tree.srcPos).value).intValue();
    		int destPos = ((Integer)((JCLiteral)tree.destPos).value).intValue();
    		int len = ((Integer)((JCLiteral)tree.len).value).intValue();
    		ListBuffer<JCStatement> unrolledStats = new ListBuffer<JCStatement>();
    		for(int i = 0; i < len; i++){
    			unrolledStats.add(make.Exec(
    					make.WiringApply(make.Indexed(src, make.Literal(srcPos+i)),
    							args.prepend(make.Indexed(dest, make.Literal(destPos+i))))
    					)
    			);
    		}
    		System.out.println("Rewritten associate statement: "+ unrolledStats.toList().toString());
    		tree.unrolled = unrolledStats.toList();
    		}
    		catch(ClassCastException e){
    			log.rawError(tree.pos, "only Integer types are allowed as indeces/sizes: "
    					+ tree);
    		}
    	}
    	else{
    		log.rawError(tree.pos, "index/size is not a literal: " + tree);
    	}
    	result = tree;
    }

    // /* (non-Javadoc)
    // * @see
    // com.sun.tools.javac.tree.TreeTranslator#visitBlock(com.sun.tools.javac.tree.JCTree.JCBlock)
    // */
    // @Override
    // public void visitBlock(JCBlock tree) {
    // assert(tree.stats.size() == 1) :
    // "blocks are supposed to be returned only from one-liner fors";
    // assert(tree.stats.head instanceof JCCapsuleArrayCall) :
    // "one liner fors should onyl contain capsule array calls";
    //
    // result = tree;
    // }
    @Override
    public void visitForLoop(JCForLoop tree) {
        List<JCStatement> unrolledLoop = executeForLoop(tree);
        result = new JCUnrolledStatement(unrolledLoop);
    }

    /**
     * @param tree
     * @return
     */
    private List<JCStatement> executeForLoop(JCForLoop tree) {
        valueEnv = valueEnv.extend();

        // put the symbols in the env;
        for (JCStatement c : tree.init)
            translate(c);

        ListBuffer<JCStatement> buffer = new ListBuffer<JCStatement>();
        boolean cond = atInterp.asBoolean(((JCLiteral)translate(copy.copy(tree.cond))).value);
        while (cond) {
            if (tree.body.getKind() == Kind.BLOCK) {
                JCBlock b = (JCBlock) tree.body;
                for (JCStatement s : b.stats) {
                    JCStatement copyOfS = translate(copy.copy(s));
                    //int b = 4 * 2
                    //a[i](b)
                    //TODO: treat case of wiring regular capsules.
                    if (copyOfS.getKind() == Kind.CAPSULE_ARRAY_CALL) {
                        buffer.add(copyOfS);
                    }
                }
            } else {
                throw new AssertionError(
                        "Don't forget to implement proper for unrolling for syslang");

            }

            translate(copy.copy(tree.step));
            cond = atInterp.asBoolean(((JCLiteral)translate(copy.copy(tree.cond))).value);
        }

        valueEnv = valueEnv.pop();
        return buffer.toList();
    }

    /**
     * @param arrayName
     * @return
     */
    private int getCapsuleArraySize(JCWireall array) {
        Name arrayName = getCapsuleArrayName(array);
        JCVariableDecl arrayAST = varDefToAstNodeEnv.lookup(arrayName);
        assert (arrayAST.vartype instanceof JCCapsuleArray) : "wireall expects capsule arrays as the first element";
        int capsuleArraySize = ((JCCapsuleArray) arrayAST.vartype).size;
        assert (capsuleArraySize > 0) : "capsule array sizes should always be > 0; something went wrong";
        return capsuleArraySize;
    }

    private int getCapsuleArraySize(JCExpression array) {
        Name arrayName = getIdentifierName(array);
        JCVariableDecl arrayAST = varDefToAstNodeEnv.lookup(arrayName);
        assert (arrayAST.vartype instanceof JCCapsuleArray) : "wireall expects capsule arrays as the first element";
        int capsuleArraySize = ((JCCapsuleArray) arrayAST.vartype).size;
        assert (capsuleArraySize > 0) : "capsule array sizes should always be > 0; something went wrong";
        return capsuleArraySize;
    }

    /**
     * @param tree
     * @return
     */
    private Name getCapsuleArrayName(JCWireall tree) {
        assert (tree.many instanceof JCIdent) : "Many2One arrays should always be referenced through identifiers";
        Name arrayName = ((JCIdent) tree.many).name;
        return arrayName;
    }

    /**
     * @param tree
     * @return
     */
    private Name getIdentifierName(JCExpression tree) {
        if(tree instanceof JCIdent){
        	Name arrayName = ((JCIdent) tree).name;
        	return arrayName;
        }
        else{
        	//TODO log the error properly:
        	//we expected an identifier, but got something else
        	log.error("unexpected.type", "identifier", tree.getKind().toString());
        	return null;
        }
    }

    // TODO: probably redundant method.
    @Override
    public void visitCapsuleDef(JCCapsuleDecl tree) {
        // FIXME: remove syso
        System.out.println("Visiting capsule def " + tree);
        super.visitCapsuleDef(tree);
        result = tree;
    }

    @Override
    public void visitSystemDef(JCSystemDecl tree) {
        // FIXME: remove syso
        System.out.println("Visiting a system decl " + tree.name.toString());
        valueEnv = new InterpEnv<Name, JCTree>();
        varDefToAstNodeEnv = new InterpEnv<Name, JCVariableDecl>();

        // translate all individual statements from the system block. This is
        // necessary because we want all subsequent blocks to enclose for
        // statements.
        ListBuffer<JCStatement> statsBuff = new ListBuffer<JCStatement>();
        for (List<JCStatement> l = tree.body.stats; l.nonEmpty(); l = l.tail) {
            statsBuff.add(translate(l.head));
        }
        tree.body.stats = statsBuff.toList();
        result = tree;
    }


    /**
     * Helper to interpret arithmetic expression trees.
     *
     * TODO: Deal with something besides ints. rename because now it supports booleans
     *
     * @author sean
     *
     */
    private class ArithTreeInterp {

        /**
         * A reference to the origin tree, for diagnostic purposes.
         */
        JCTree tree;

        public JCTree interp(final JCTree tree, final JCLiteral lhs,
                final JCLiteral rhs) {
            this.tree = tree; // bind the tree we are current working on;
            final JCTree result;

            switch (tree.getTag()) {
            case PLUS:
                result = interpPlus(lhs, rhs);
                break;
            case MINUS:
                result = interpMinus(lhs, rhs);
                break;
            case MUL:
                result = interpMul(lhs, rhs);
                break;
            case DIV:
                result = interpDiv(lhs, rhs);
                break;
            case MOD:
                result = interpMod(lhs, rhs);
                break;
            case LT:
                result = interpretLT(lhs, rhs);
                break;
            case LE:
                result = interpretLE(lhs, rhs);
                break;
            case GT:
                result = interpretGT(lhs, rhs);
                break;
            case GE:
                result = interpretGE(lhs, rhs);
                break;
            case EQ:
                result = interpretEQ(lhs, rhs);
                break;
            case NE:
                result = interpretNE(lhs, rhs);
                break;
            case AND:
                result = interpretAND(lhs, rhs);
                break;
            case OR:
                result = interpretOR(lhs, rhs);
                break;

            // TODO: Other cases?
            default:
                result = tree;
            }

            // get rid of the reference when we are done interpretting it.
            // prevents stale tree type errors.
            this.tree = null;
            return result;
        }
/**
         * @param lhs
         * @param rhs
         * @return
         */
        private JCTree interpretOR(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != rhs.typetag) {
                // FIXME rawWarning
                log.rawWarning(tree.pos, tree
                        + "did not have the same typetag for lhs and rhs");
                return tree;
            } else {
                switch (lhs.typetag) {
                case TypeTags.BOOLEAN:
                    boolean vLHS = asBoolean(lhs.value);
                    boolean vRHS = asBoolean(rhs.value);
                    make.pos = tree.pos;
                    return make.Literal(lhs.typetag,
                            Boolean.valueOf(vLHS || vRHS));
                default:
                    return tree;
                }
            }
        }
/**
         * @param lhs
         * @param rhs
         * @return
         */
        private JCTree interpretAND(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != rhs.typetag) {
                // FIXME rawWarning
                log.rawWarning(tree.pos, tree
                        + "did not have the same typetag for lhs and rhs");
                return tree;
            } else {
                switch (lhs.typetag) {
                case TypeTags.BOOLEAN:
                    boolean vLHS = asBoolean(lhs.value);
                    boolean vRHS = asBoolean(rhs.value);
                    make.pos = tree.pos;
                    return make.Literal(lhs.typetag,
                            Boolean.valueOf(vLHS && vRHS));
                default:
                    return tree;
                }
            }
        }
/**
         * @param lhs
         * @param rhs
         * @return
         */
        private JCTree interpretNE(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != rhs.typetag) {
                // FIXME rawWarning
                log.rawWarning(tree.pos, tree
                        + "did not have the same typetag for lhs and rhs");
                return tree;
            } else {
                switch (lhs.typetag) {
                case TypeTags.INT:
                    int vLHS = asInt(lhs.value);
                    int vRHS = asInt(rhs.value);
                    make.pos = tree.pos;
                    return make.Literal(lhs.typetag,
                            Boolean.valueOf(vLHS != vRHS));
                default:
                    return tree;
                }
            }
        }
/**
         * @param lhs
         * @param rhs
         * @return
         */
        private JCTree interpretEQ(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != rhs.typetag) {
                // FIXME rawWarning
                log.rawWarning(tree.pos, tree
                        + "did not have the same typetag for lhs and rhs");
                return tree;
            } else {
                switch (lhs.typetag) {
                case TypeTags.INT:
                    int vLHS = asInt(lhs.value);
                    int vRHS = asInt(rhs.value);
                    make.pos = tree.pos;
                    return make.Literal(lhs.typetag,
                            Boolean.valueOf(vLHS == vRHS));
                default:
                    return tree;
                }
            }
        }
/**
         * @param lhs
         * @param rhs
         * @return
         */
        private JCTree interpretGE(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != rhs.typetag) {
                // FIXME rawWarning
                log.rawWarning(tree.pos, tree
                        + "did not have the same typetag for lhs and rhs");
                return tree;
            } else {
                switch (lhs.typetag) {
                case TypeTags.INT:
                    int vLHS = asInt(lhs.value);
                    int vRHS = asInt(rhs.value);
                    make.pos = tree.pos;
                    return make.Literal(lhs.typetag,
                            Boolean.valueOf(vLHS >= vRHS));
                default:
                    return tree;
                }
            }
        }
/**
         * @param lhs
         * @param rhs
         * @return
         */
        private JCTree interpretGT(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != rhs.typetag) {
                // FIXME rawWarning
                log.rawWarning(tree.pos, tree
                        + "did not have the same typetag for lhs and rhs");
                return tree;
            } else {
                switch (lhs.typetag) {
                case TypeTags.INT:
                    int vLHS = asInt(lhs.value);
                    int vRHS = asInt(rhs.value);
                    make.pos = tree.pos;
                    return make.Literal(lhs.typetag,
                            Boolean.valueOf(vLHS > vRHS));
                default:
                    return tree;
                }
            }
        }
/**
         * @param lhs
         * @param rhs
         * @return
         */
        private JCTree interpretLE(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != rhs.typetag) {
                // FIXME rawWarning
                log.rawWarning(tree.pos, tree
                        + "did not have the same typetag for lhs and rhs");
                return tree;
            } else {
                switch (lhs.typetag) {
                case TypeTags.INT:
                    int vLHS = asInt(lhs.value);
                    int vRHS = asInt(rhs.value);
                    make.pos = tree.pos;
                    return make.Literal(lhs.typetag,
                            Boolean.valueOf(vLHS <= vRHS));
                default:
                    return tree;
                }
            }
        }
/**
         * @return
         */
        private JCTree interpretLT(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != rhs.typetag) {
                // FIXME rawWarning
                log.rawWarning(tree.pos, tree
                        + "did not have the same typetag for lhs and rhs");
                return tree;
            } else {
                switch (lhs.typetag) {
                case TypeTags.INT:
                    int vLHS = asInt(lhs.value);
                    int vRHS = asInt(rhs.value);
                    make.pos = tree.pos;
                    return make.Literal(lhs.typetag,
                            Boolean.valueOf(vLHS < vRHS));
                default:
                    return tree;
                }
            }
        }


        final JCTree interpPlus(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != rhs.typetag) {
                // FIXME rawWarning
                log.rawWarning(tree.pos, tree
                        + "did not have the same typetag for lhs and rhs");
                return tree;
            } else {
                switch (lhs.typetag) {
                case TypeTags.INT:
                    int vLHS = asInt(lhs.value);
                    int vRHS = asInt(rhs.value);
                    make.pos = tree.pos;
                    return make.Literal(lhs.typetag,
                            Integer.valueOf(vLHS + vRHS));
                default:
                    return tree;
                }
            }
        }

        final JCTree interpMinus(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != rhs.typetag) {
                // FIXME rawWarning
                log.rawWarning(tree.pos, tree
                        + "did not have the same typetag for lhs and rhs");
                return tree;
            } else {
                switch (lhs.typetag) {
                case TypeTags.INT:
                    int vLHS = asInt(lhs.value);
                    int vRHS = asInt(rhs.value);
                    make.pos = tree.pos;
                    return make.Literal(lhs.typetag,
                            Integer.valueOf(vLHS - vRHS));
                default:
                    return tree;
                }
            }
        }

        final JCTree interpMul(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != rhs.typetag) {
                // FIXME rawWarning
                log.rawWarning(tree.pos, tree
                        + "did not have the same typetag for lhs and rhs");
                return tree;
            } else {
                switch (lhs.typetag) {
                case TypeTags.INT:
                    int vLHS = asInt(lhs.value);
                    int vRHS = asInt(rhs.value);
                    make.pos = tree.pos;
                    return make.Literal(lhs.typetag,
                            Integer.valueOf(vLHS * vRHS));
                default:
                    return tree;
                }
            }
        }

        final JCTree interpDiv(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != rhs.typetag) {
                // FIXME rawWarning
                log.rawWarning(tree.pos, tree
                        + "did not have the same typetag for lhs and rhs");
                return tree;
            } else {
                switch (lhs.typetag) {
                case TypeTags.INT:
                    int vLHS = asInt(lhs.value);
                    int vRHS = asInt(rhs.value);
                    make.pos = tree.pos;
                    return make.Literal(lhs.typetag,
                            Integer.valueOf(vLHS / vRHS));
                default:
                    return tree;
                }
            }
        }

        final JCTree interpMod(JCLiteral lhs, JCLiteral rhs) {
            if (lhs.typetag != rhs.typetag) {
                // FIXME rawWarning
                log.rawWarning(tree.pos, tree
                        + "did not have the same typetag for lhs and rhs");
                return tree;
            } else {
                switch (lhs.typetag) {
                case TypeTags.INT:
                    int vLHS = asInt(lhs.value);
                    int vRHS = asInt(rhs.value);
                    make.pos = tree.pos;
                    return make.Literal(lhs.typetag,
                            Integer.valueOf(vLHS % vRHS));
                default:
                    return tree;
                }
            }
        }

        final int asInt(Object obj) {
            // FIXME Possible class cast exception.
            return (Integer) obj;
        }

        final boolean asBoolean(Object obj) {
            return (Boolean) obj;
        }
    }

    /**
     * Standard linked environment for the interpretor
     *
     * @author sean
     *
     */
    private static class InterpEnv<K, V> {
        private final HashMap<K, V> table;
        private final InterpEnv<K, V> parent;

        public InterpEnv() {
            table = new HashMap<K, V>();
            parent = null;
        }

        private InterpEnv(InterpEnv<K, V> other) {
            table = new HashMap<K, V>();
            parent = other;
        }

        public void bind(K k, V v) {
            table.put(k, v);
        }

        public V lookup(K k) {
            V v = table.get(k);
            if (v == null && parent != null) {
                return parent.lookup(k);
            } else {
                return v;
            }
        }

        public InterpEnv<K, V> extend() {
            return new InterpEnv<K, V>(this);
        }

        public InterpEnv<K, V> pop() {
            return parent;
        }
    }

    // FIXME: do something to have a generic solution for both wireall and for;
    private static class JCUnrolledStatement extends JCStatement {
        public List<JCStatement> unrolled;

        /**
         * @param unrolledLoop
         */
        public JCUnrolledStatement(List<JCStatement> unrolledLoop) {
            unrolled = unrolledLoop;
        }

        @Override
        public Kind getKind() {
            return null;
        }

        /* (non-Javadoc)
         * @see com.sun.tools.javac.tree.JCTree#getTag()
         */
        @Override
        public Tag getTag() {
            // TODO Auto-generated method stub
            return null;
        }

        /* (non-Javadoc)
         * @see com.sun.tools.javac.tree.JCTree#accept(com.sun.tools.javac.tree.JCTree.Visitor)
         */
        @Override
        public void accept(Visitor v) {
            // TODO Auto-generated method stub

        }

        /* (non-Javadoc)
         * @see com.sun.tools.javac.tree.JCTree#accept(com.sun.source.tree.TreeVisitor, java.lang.Object)
         */
        @Override
        public <R, D> R accept(TreeVisitor<R, D> v, D d) {
            // TODO Auto-generated method stub
            return null;
        }
    }


}
