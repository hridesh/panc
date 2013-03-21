package org.paninij.analysis;
/**
 * 
 * @author gupadhyaya
 *
 */
public interface Costs {

    /** Byte code costs.
     */
    int illegal         = -1,
        nop             = 0,
        aconst_null     = 4,
        iconst_m1       = 4,
        iconst_0        = 4,
        iconst_1        = 4,
        iconst_2        = 4,
        iconst_3        = 4,
        iconst_4        = 4,
        iconst_5        = 4,
        lconst_0        = 8,
        lconst_1        = 8,
        fconst_0        = 4,
        fconst_1        = 4,
        fconst_2        = 4,
        dconst_0        = 5,
        dconst_1        = 5,
        bipush          = 4,
        sipush          = 4,
        ldc1            = 2,
        ldc2            = 5,
        ldc2w           = 10,
        iload           = 8,
        lload           = 8,
        fload           = 8,
        dload           = 10,
        aload           = 10,
        iload_0         = 8,
        iload_1         = 8,
        iload_2         = 8,
        iload_3         = 8,
        lload_0         = 8,
        lload_1         = 8,
        lload_2         = 8,
        lload_3         = 8,
        fload_0         = 8,
        fload_1         = 8,
        fload_2         = 8,
        fload_3         = 8,
        dload_0         = 10,
        dload_1         = 10,
        dload_2         = 10,
        dload_3         = 10,
        aload_0         = 10,
        aload_1         = 10,
        aload_2         = 10,
        aload_3         = 10,
        iaload          = 10,
        laload          = 10,
        faload          = 10,
        daload          = 12,
        aaload          = 12,
        baload          = 12,
        caload          = 12,
        saload          = 12,
        istore          = 8,
        lstore          = 8,
        fstore          = 8,
        dstore          = 8,
        astore          = 8,
        istore_0        = 8,
        istore_1        = 8,
        istore_2        = 8,
        istore_3        = 8,
        lstore_0        = 8,
        lstore_1        = 8,
        lstore_2        = 8,
        lstore_3        = 8,
        fstore_0        = 8,
        fstore_1        = 8,
        fstore_2        = 8,
        fstore_3        = 8,
        dstore_0        = 8,
        dstore_1        = 8,
        dstore_2        = 8,
        dstore_3        = 8,
        astore_0        = 20,
        astore_1        = 20,
        astore_2        = 20,
        astore_3        = 20,
        iastore         = 20,
        lastore         = 20,
        fastore         = 20,
        dastore         = 20,
        aastore         = 20,
        bastore         = 20,
        castore         = 20,
        sastore         = 20,
        pop             = 4,
        pop2            = 8,
        dup             = 4,
        dup_x1          = 12,
        dup_x2          = 16,
        dup2            = 12,
        dup2_x1         = 16,
        dup2_x2         = 22,
        swap            = 10,
        iadd            = 5,
        ladd            = 5,
        fadd            = 5,
        dadd            = 5,
        isub            = 5,
        lsub            = 5,
        fsub            = 5,
        dsub            = 5,
        imul            = 51,
        lmul            = 51,
        fmul            = 51,
        dmul            = 51,
        idiv            = 52,
        ldiv            = 52,
        fdiv            = 52,
        ddiv            = 52,
        imod            = 52,
        lmod            = 52,
        fmod            = 52,
        dmod            = 52,
        ineg            = 2,
        lneg            = 2,
        fneg            = 2,
        dneg            = 3,
        ishl            = 8,
        lshl            = 8,
        ishr            = 8,
        lshr            = 8,
        iushr           = 8,
        lushr           = 8,
        iand            = 5,
        land            = 5,
        ior             = 5,
        lor             = 5,
        ixor            = 5,
        lxor            = 5,
        iinc            = 3,
        i2l             = 5,
        i2f             = 5,
        i2d             = 5,
        l2i             = 5,
        l2f             = 5,
        l2d             = 5,
        f2i             = 5,
        f2l             = 5,
        f2d             = 5,
        d2i             = 5,
        d2l             = 5,
        d2f             = 5,
        int2byte        = 5,
        int2char        = 5,
        int2short       = 5,
        lcmp            = 8,
        fcmpl           = 8,
        fcmpg           = 8,
        dcmpl           = 8,
        dcmpg           = 8,
        ifeq            = 12,
        ifne            = 12,
        iflt            = 12,
        ifge            = 12,
        ifgt            = 12,
        ifle            = 12,
        if_icmpeq       = 12,
        if_icmpne       = 12,
        if_icmplt       = 12,
        if_icmpge       = 12,
        if_icmpgt       = 12,
        if_icmple       = 12,
        if_acmpeq       = 12,
        if_acmpne       = 12,
        goto_           = 12,
        jsr             = 0,
        ret             = 21,
        tableswitch     = 0,
        lookupswitch    = 0,
        ireturn         = 21,
        lreturn         = 21,
        freturn         = 21,
        dreturn         = 21,
        areturn         = 21,
        return_         = 21,
        getstatic       = 36,
        putstatic       = 39,
        getfield        = 39,
        putfield        = 39,
        invokevirtual   = 60,
        invokespecial   = 56,
        invokestatic    = 60,
        invokeinterface = 60,
        invokedynamic   = 60,
        new_            = 33,
        newarray        = 33,
        anewarray       = 33,
        arraylength     = 0,
        athrow          = 0,
        checkcast       = 0,
        instanceof_     = 0,
        monitorenter    = 0,
        monitorexit     = 0,
        wide            = 0,
        multianewarray  = 0,
        if_acmp_null    = 8,
        if_acmp_nonnull = 8,
        goto_w          = 5,
        jsr_w           = 0,
        breakpoint      = 0,
        ByteCodeCount   = 0;

    /** Virtual instruction codes; used for constant folding.
     */
    int string_add      = 256,  // string +
        bool_not        = 257,  // boolean !
        bool_and        = 258,  // boolean &&
        bool_or         = 259;  // boolean ||

    /** Virtual opcodes; used for shifts with long shiftcount
     */
    int ishll           = 270,  // int shift left with long count
        lshll           = 271,  // long shift left with long count
        ishrl           = 272,  // int shift right with long count
        lshrl           = 273,  // long shift right with long count
        iushrl          = 274,  // int unsigned shift right with long count
        lushrl          = 275;  // long unsigned shift right with long count

    /** Virtual opcode for null reference checks
     */
    int nullchk         = 276;  // return operand if non-null,
                                // otherwise throw NullPointerException.

    /** Virtual opcode for disallowed operations.
     */
    int error           = 277;

    /** All conditional jumps come in pairs. To streamline the
     *  treatment of jumps, we also introduce a negation of an
     *  unconditional jump. That opcode happens to be jsr.
     */
    int dontgoto        = jsr;

    /** Shift and mask constants for shifting prefix instructions.
     *  a pair of instruction codes such as LCMP ; IFEQ is encoded
     *  in Symtab as (LCMP << preShift) + IFEQ.
     */
    int preShift        = 9;
    int preMask         = (1 << preShift) - 1;

    /** Type codes.
     */
    int INTcode         = 0,
        LONGcode        = 1,
        FLOATcode       = 2,
        DOUBLEcode      = 3,
        OBJECTcode      = 4,
        BYTEcode        = 5,
        CHARcode        = 6,
        SHORTcode       = 7,
        VOIDcode        = 8,
        TypeCodeCount   = 9;

    static final String[] typecodeNames = {
        "int",
        "long",
        "float",
        "double",
        "object",
        "byte",
        "char",
        "short",
        "void",
        "oops"
    };
    
    static final int THRESHOLD = 1000;
}
