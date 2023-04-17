package edu.westminstercollege.cs.jade.classfile.instruction;

import static edu.westminstercollege.cs.jade.classfile.instruction.OperandType.*;

import java.util.*;

public enum Opcode {

    AALOAD(50),
    AASTORE(83),
    ACONST_NULL(1),
    ALOAD(25, List.of(U8), List.of(U16)),
    ALOAD_0(42),
    ALOAD_1(43),
    ALOAD_2(44),
    ALOAD_3(45),
    ANEWARRAY(189, RefType),
    ARETURN(176),
    ARRAYLENGTH(190),
    ASTORE(58, List.of(U8), List.of(U16)),
    ASTORE_0(75),
    ASTORE_1(76),
    ASTORE_2(77),
    ASTORE_3(78),
    ATHROW(191),
    BALOAD(51),
    BASTORE(84),
    BIPUSH(16, S8),
    CALOAD(52),
    CASTORE(85),
    CHECKCAST(192, RefType),
    D2F(144),
    D2I(142),
    D2L(143),
    DADD(99),
    DALOAD(49),
    DASTORE(82),
    DCMPG(152),
    DCMPL(151),
    DCONST_0(14),
    DCONST_1(15),
    DDIV(111),
    DLOAD(24, List.of(U8), List.of(U16)),
    DLOAD_0(38),
    DLOAD_1(39),
    DLOAD_2(40),
    DLOAD_3(41),
    DMUL(107),
    DNEG(119),
    DREM(115),
    DRETURN(175),
    DSTORE(57, List.of(U8), List.of(U16)),
    DSTORE_0(71),
    DSTORE_1(72),
    DSTORE_2(73),
    DSTORE_3(74),
    DSUB(103),
    DUP(89),
    DUP_X1(90),
    DUP_X2(91),
    DUP2(92),
    DUP2_X1(93),
    DUP2_X2(94),
    F2D(141),
    F2I(139),
    F2L(140),
    FADD(98),
    FALOAD(48),
    FASTORE(81),
    FCMPG(150),
    FCMPL(149),
    FCONST_0(11),
    FCONST_1(12),
    FCONST_2(13),
    FDIV(110),
    FLOAD(23, List.of(U8), List.of(U16)),
    FLOAD_0(34),
    FLOAD_1(35),
    FLOAD_2(36),
    FLOAD_3(37),
    FMUL(106),
    FNEG(118),
    FREM(114),
    FRETURN(174),
    FSTORE(56, List.of(U8), List.of(U16)),
    FSTORE_0(67),
    FSTORE_1(68),
    FSTORE_2(69),
    FSTORE_3(70),
    FSUB(102),
    GETFIELD(180, Field),
    GETSTATIC(178, Field),
    GOTO(167, BranchOffset16),
    GOTO_W(200, BranchOffset32),
    I2B(145),
    I2C(146),
    I2D(135),
    I2F(134),
    I2L(133),
    I2S(147),
    IADD(96),
    IALOAD(46),
    IAND(126),
    IASTORE(79),
    ICONST_M1(2),
    ICONST_0(3),
    ICONST_1(4),
    ICONST_2(5),
    ICONST_3(6),
    ICONST_4(7),
    ICONST_5(8),
    IDIV(108),
    IF_ACMPEQ(165, BranchOffset16),
    IF_ACMPNE(166, BranchOffset16),
    IF_ICMPEQ(159, BranchOffset16),
    IF_ICMPNE(160, BranchOffset16),
    IF_ICMPLT(161, BranchOffset16),
    IF_ICMPGE(162, BranchOffset16),
    IF_ICMPGT(163, BranchOffset16),
    IF_ICMPLE(164, BranchOffset16),
    IFEQ(153, BranchOffset16),
    IFNE(154, BranchOffset16),
    IFLT(155, BranchOffset16),
    IFGE(156, BranchOffset16),
    IFGT(157, BranchOffset16),
    IFLE(158, BranchOffset16),
    IFNONNULL(199, BranchOffset16),
    IFNULL(198, BranchOffset16),
    IINC(132, List.of(U8, S8), List.of(U16, S16)),
    ILOAD(21, List.of(U8), List.of(U16)),
    ILOAD_0(26),
    ILOAD_1(27),
    ILOAD_2(28),
    ILOAD_3(29),
    IMUL(104),
    INEG(116),
    INSTANCEOF(193, RefType),
    INVOKEDYNAMIC(186, DynamicCallSite, U8, U8),
    INVOKEINTERFACE(185, Method, U8, U8),
    INVOKESPECIAL(183, Method),
    INVOKESTATIC(184, Method),
    INVOKEVIRTUAL(182, Method),
    IOR(128),
    IREM(112),
    IRETURN(172),
    ISHL(120),
    ISHR(122),
    ISTORE(54, List.of(U8), List.of(U16)),
    ISTORE_0(59),
    ISTORE_1(60),
    ISTORE_2(61),
    ISTORE_3(62),
    ISUB(100),
    IUSHR(124),
    IXOR(130),
    JSR(168, BranchOffset16),
    JWR_W(201, BranchOffset32),
    L2D(138),
    L2F(137),
    L2I(136),
    LADD(97),
    LALOAD(47),
    LAND(127),
    LASTORE(80),
    LCMP(148),
    LCONST_0(9),
    LCONST_1(10),
    LDC(18, Imm8),
    LDC_W(19, Imm16),
    LDC2_W(20, Imm16),
    LDIV(109),
    LLOAD(22, List.of(U8), List.of(U16)),
    LLOAD_0(30),
    LLOAD_1(31),
    LLOAD_2(32),
    LLOAD_3(33),
    LMUL(105),
    LNEG(117),
    LOOKUPSWITCH(171, LUT),
    LOR(129),
    LREM(113),
    LRETURN(173),
    LSHL(121),
    LSHR(123),
    LSTORE(55, List.of(U8), List.of(U16)),
    LSTORE_0(63),
    LSTORE_1(64),
    LSTORE_2(65),
    LSTORE_3(66),
    LSUB(101),
    LUSHR(125),
    LXOR(131),
    MONITORENTER(194),
    MONITOREXIT(195),
    MULTIANEWARRAY(197, RefType, U8),
    NEW(187, RefType),
    NEWARRAY(188, AType),
    NOP(0),
    POP(87),
    POP2(88),
    PUTFIELD(181, Field),
    PUTSTATIC(179, Field),
    RET(169, List.of(U8), List.of(U16)),
    RETURN(177),
    SALOAD(53),
    SASTORE(86),
    SIPUSH(17, S16),
    SWAP(95),
    TABLESWITCH(170, JT),
    WIDE(196);

    private final int value;
    private final List<OperandType> operands;
    private final List<OperandType> wideOperands;
    private static Map<Integer, Opcode> opcodes = new HashMap<>(256);

    static {
        for (var opcode : Opcode.values()) {
            if (opcodes.containsKey(opcode.value))
                throw new RuntimeException(String.format("Multiple opcodes of value %d!\n", opcode.value));
            opcodes.put(opcode.value, opcode);
        }
    }

    private Opcode(int value, OperandType... operands) {
        this.value = value;
        this.operands = Arrays.asList(operands);
        this.wideOperands = null;
    }

    private Opcode(int value, List<OperandType> operands, List<OperandType> wideOperands) {
        this.value = value;
        this.operands = operands;
        this.wideOperands = wideOperands;
    }

    public int value() {
        return value;
    }

    public String mnemonic() {
        return name().toLowerCase();
    }

    public List<OperandType> operandTypes() {
        return operands;
    }

    public List<OperandType> wideOperandTypes() {
        if (!isWidenable())
            throw new IllegalStateException("Opcode " + mnemonic() + " cannot be widened!");
        return wideOperands;
    }

    public boolean isWidenable() {
        return (wideOperands != null);
    }

    public static Optional<Opcode> of(int num) {
        return Optional.ofNullable(opcodes.get(num));
    }

    public static Optional<Opcode> of(String mnemonic) {
        if (!mnemonic.equals(mnemonic.toLowerCase()))
            return Optional.empty();
        try {
            return Optional.of(valueOf(mnemonic.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
