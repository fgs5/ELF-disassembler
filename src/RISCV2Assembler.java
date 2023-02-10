import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RISCV2Assembler {
    private final List<String> codes;
    private final ELFParser source;
    private int address;

    private final HashMap<String, String> systemRegisters = new HashMap<>();

    private final String[] registers = {
            "zero", "ra", "sp",
            "gp", "tp", "t0",
            "t1", "t2", "s0",
            "s1", "a0", "a1",
            "a2", "a3", "a4",
            "a5", "a6", "a7",
            "s2", "s3", "s4",
            "s5", "s6", "s7",
            "s8", "s9", "s10",
            "s11", "t3", "t4",
            "t5", "t6"
    };

    private final HashMap<String, String> cRegisters = new HashMap<>(Map.of(
            "000", "s0",
            "001", "s1",
            "010", "a0",
            "011", "a1",
            "100", "a2",
            "101", "a3",
            "110", "a4",
            "111", "a5"
    ));

    private final HashMap<String, String> rCommands = new HashMap<>(Map.of(
            "0000000000", "ADD",
            "0000000001", "SLL",
            "0000000010", "SLT",
            "0000000011", "SLTU",
            "0000000100", "XOR",
            "0000000101", "SRL",
            "0000000110", "OR",
            "0000000111", "AND",
            "0100000000", "SUB",
            "0100000101", "SRA"
    ));

    private final HashMap<String, String> iCommands = new HashMap<>(Map.of(
            "000", "ADDI",
            "010", "SLTI",
            "011", "SLTIU",
            "100", "XORI",
            "110", "ORI",
            "111", "ANDI"
    ));

    private final HashMap<String, String> loadCommands = new HashMap<>(Map.of(
            "000", "LB",
            "001", "LH",
            "010", "LW",
            "100", "LBU",
            "101", "LHU"
    ));

    private final HashMap<String, String> sCommands = new HashMap<>(Map.of(
            "000", "SB",
            "001", "SH",
            "010", "SW"
    ));

    private final HashMap<String, String> sbCommands = new HashMap<>(Map.of(
            "000", "BEQ",
            "001", "BNE",
            "100", "BLT",
            "101", "BGE",
            "110", "BLTU",
            "111", "BGEU"
    ));

    private final HashMap<String, String> q0Commands = new HashMap<>(Map.of(
            "000", "C.ADDI4SPN",
            "001", "C.FLD",
            "010", "C.LW",
            "011", "C.FLW",
            "101", "C.FSD",
            "110", "C.SW",
            "111", "C.FSW"
    ));

    private void fillHashMaps() {
        rCommands.putAll(Map.of(
                "0000001000", "MUL",
                "0000001100", "DIV",
                "0000001001", "MULH",
                "0000001011", "MULHU",
                "0000001010", "MULHSU",
                "0000001111", "REMU",
                "0000001101", "DIVU",
                "0000001110", "REM"
        ));
        systemRegisters.putAll(Map.of(
                "000000000001", "fflags", "000000000010", "frm", "000000000011", "fcsr",
                "110000000000", "cycle", "110000000001", "time", "110000000010", "instret",
                "110010000000", "cycleh", "110010000001", "timeh", "110010000010", "instreth",
                "011110110011", "dscratch1"));
        systemRegisters.putAll(Map.of(
                "000100000000", "sstatus", "000100000100", "sie", "000100000101", "stvec",
                "000100000110", "scounteren", "000100001010", "senvcfg",
                "000101000000", "sscratch", "000101000001", "sepc", "000101000010", "scause",
                "000101000011", "stval", "011110110010", "dscratch0"));
        systemRegisters.putAll(Map.of("000101000100", "sip", "000110000000", "satp",
                "010110101000", "scontext", "011000000000", "hstatus", "011000000010", "hedeleg",
                "011000000011", "hidelef", "011000000100", "hie", "011000000110", "hcounteren",
                "011000000111", "hgeie", "011001000011", "htval"));
        systemRegisters.putAll(Map.of("011001000100", "hip",
                "011001000101", "hvip", "011001001010", "htinst", "111000010010", "hgeip",
                "011000001010", "henvcfg", "011000011010", "henvcfgh", "011010000000", "hgatp",
                "011010101000", "hcontext", "011000000101", "htimedelta",
                "011000010101", "htimedeltah"));
        systemRegisters.putAll(Map.of("001000000000", "vsstatus",
                "001000000100", "vsie", "001000000101", "vstvec", "001001000000", "vsscratch",
                "001001000001", "vsepc", "001001000010", "vscause", "001001000011", "vstval",
                "001001000100", "vsip", "001010000000", "vsatp",
                "111100010001", "mvendorid"));
        systemRegisters.putAll(Map.of("111100010010", "marchid", "111100010011", "mimpid",
                "111100010100", "mhartid", "111100010101", "mconfigptr",
                "001100000000", "mstatus", "001100000001", "misa", "001100000010", "medeleg",
                "001100000011", "mideleg", "001100000100", "mie", "001100000101", "mtvec"));
        systemRegisters.putAll(Map.of("001100000110", "mcounteren",
                "001100010000", "mstatush", "001101000000", "mscratch", "001101000001", "mepc",
                "001101000010", "mcause", "001101000011", "mtval", "001101000100", "mip",
                "001101001010", "mtinst", "001101001011", "mtval2", "001100001010", "menvcfg"));
        systemRegisters.putAll(Map.of("001100011010", "menvcfgh", "011101000111", "mseccfg",
                "011101010111", "mseccfgh", "101100000000", "mcycle", "101100000010", "minstret",
                "101110000000", "mcycleh", "101110000010", "minstreth"));
        systemRegisters.putAll(Map.of("001100100000", "mcountinhibit", "011110100000","tselect",
                "011110100001", "tdata1", "011110100010", "tdata2", "011110100011", "tdata3",
                "011110101000", "mcontext", "011110110000", "dcsr", "011110110001", "dpc"));

        int temp = 12 * 256 + 3;
        for (int i = 0; i < 29; i++) {
            systemRegisters.put(source.toBin(temp + i), "hpmcounter" + (i + 3));
        }

        temp = 12 * 256 + 8 * 16 + 3;
        for (int i = 0; i < 29; i++) {
            systemRegisters.put(source.toBin(temp + i), "hpmcounter" + (i + 3) + "h");
        }

        temp = 3 * 256 + 16 * 10;
        for (int i = 0; i < 16; i++) {
            systemRegisters.put(source.toBin(temp + i), "pmpcfg0" + i);
        }

        temp = 3 * 256 + 16 * 11;
        for (int i = 0; i < 64; i++) {
            systemRegisters.put(source.toBin(temp + i), "pmpaddr0" + i);
        }

        temp = 11 * 256 + 3;
        for (int i = 0; i < 29; i++) {
            systemRegisters.put(source.toBin(temp + i), "mhpmcounter" + (i + 3));
        }

        temp = 11 * 256 + 16 * 8 + 3;
        for (int i = 0; i < 29; i++) {
            systemRegisters.put(source.toBin(temp + i), "mhpmcounter" + (i + 3) + "h");
        }

        temp = 3 * 256 + 2 * 16 + 3;
        for (int i = 0; i < 29; i++) {
            systemRegisters.put(source.toBin(temp + i), "mhpmevent" + (i + 3));
        }
    }

    public RISCV2Assembler(List<String> codes, ELFParser source) {
        this.source = source;
        this.codes = codes;
        this.address = (int) source.getTextAddress();
    }

    public StringBuilder convert() {
        fillHashMaps();
        run();
        address = (int) source.getTextAddress();
        return run();
    }

    private StringBuilder run() {
        StringBuilder result = new StringBuilder();
        for (String code : codes) {
            if (code.length() == 16) {
                String temp = code.substring(14);
                int arg0;
                String arg1;
                String arg2 = "";
                String arg3;
                String arg4;
                String arg5;
                String part0, part2, part4;
                arg0 = address;
                arg1 = addSym();
                switch (temp) {
                    case "00":
                        part0 = code.substring(0, 3);
                        part2 = code.substring(6, 9);
                        part4 = code.substring(11, 14);
                        arg2 = q0Commands.get(part0);
                        if (part0.charAt(0) == '0') {
                            arg3 = cRegisters.get(part4);
                            arg4 = cRegisters.get(part2);
                        } else {
                            arg3 = cRegisters.get(part2);
                            arg4 = cRegisters.get(part4);
                        }
                        switch (part0) {
                            case "000":
                                arg4 = String.valueOf(source.fromAbsBin(
                                        "" + code.substring(5, 9) +
                                                code.substring(3, 5) + code.charAt(10) + code.charAt(9) + "00")
                                );
                                result.append(toFormat(arg0, arg1, arg2, arg3, "sp", arg4));
                                break;
                            case "001":
                            case "101":
                                arg5 = String.valueOf(source.fromAbsBin("" + code.substring(9, 11) +
                                        code.substring(3, 6) + "000"));
                                result.append(lsToFormat(arg0, arg1, arg2, arg3, arg4, arg5));
                                break;
                            case "011":
                            case "111":
                            case "010":
                            case "110":
                                arg5 = String.valueOf(source.fromAbsBin(
                                        "" + code.charAt(10) +
                                                code.substring(3, 6) + code.charAt(9) + "00")
                                );
                                result.append(lsToFormat(arg0, arg1, arg2, arg3, arg4, arg5));
                                break;
                            default:
                                arg2 = "unknown_command";
                                result.append(toFormat(arg0, arg1, arg2));
                                break;
                        }
                        break;
                    case "01":
                        String fiveFourZeroImm = "" + code.charAt(3) + code.substring(9, 14);
                        String repeatedString = String.valueOf(source.fromBin(
                                fiveFourZeroImm)
                        );
                        part0 = code.substring(0, 3);
                        String number = "" + code.charAt(3) +
                                code.charAt(7) +
                                code.charAt(5) + code.charAt(6) +
                                code.charAt(9) + code.charAt(8) +
                                code.charAt(13) +
                                code.charAt(4) +
                                code.substring(10, 13) + "0";
                        String number1 = "" + code.charAt(3) + code.substring(9, 11) +
                                code.charAt(13) +
                                code.substring(4, 6) +
                                code.substring(11, 13) + "0";
                        switch (part0) {
                            case "000":
                                if (code.startsWith("00000", 4)) {
                                    arg2 = "C.NOP";
                                    arg3 = repeatedString;
                                    result.append(toFormat(arg0, arg1, arg2, arg3));
                                } else {
                                    arg2 = "C.ADDI";
                                    arg3 = getRegister(code.substring(4, 9));
                                    arg4 = repeatedString;
                                    result.append(toFormat(arg0, arg1, arg2, arg3, arg4));
                                }
                                break;
                            case "001":
                                arg2 = "C.JAL";
                                int la = source.fromBin(number);
                                arg3 = String.valueOf(la);
                                source.addSym(la + address - 4);
                                result.append(toJumpFormat(arg0, arg1, arg2, arg3, la + address - 4));
                                break;
                            case "010":
                                arg2 = "C.LI";
                                arg3 = getRegister(code.substring(4, 9));
                                arg4 = repeatedString;
                                result.append(toFormat(arg0, arg1, arg2, arg3, arg4));
                                break;
                            case "011":
                                String regOrTwo = code.substring(4, 9);
                                if (regOrTwo.equals("00010")) {
                                    arg2 = "C.ADDI16SP";
                                    arg3 = String.valueOf(source.fromBin(
                                            "" + code.charAt(3) +
                                                    code.substring(11, 13) +
                                                    code.charAt(10) +
                                                    code.charAt(13) +
                                                    code.charAt(9) + "0000"

                                    ));
                                    result.append(toFormat(arg0, arg1, arg2, "sp", arg3));
                                } else {
                                    arg2 = "C.LUI";
                                    arg3 = getRegister(regOrTwo);
                                    arg4 = String.valueOf(source.fromBin(
                                            "" + code.charAt(3) + code.substring(9, 14) + "000000000000"
                                    ));
                                    result.append(toFormat(arg0, arg1, arg2, arg3, arg4));
                                }
                                break;
                            case "100":
                                String subCode = code.substring(4, 6);
                                String destReg0 = cRegisters.get(code.substring(6, 9));
                                String srcReg = cRegisters.get(code.substring(11, 14));
                                switch (subCode) {
                                    case "00":
                                        if (!fiveFourZeroImm.equals("00000")) {
                                            arg2 = "C.SRLI64";
                                            arg3 = destReg0;
                                            result.append(toFormat(arg0, arg1, arg2, arg3));
                                        } else {
                                            arg2 = "C.SRLI";
                                            arg3 = destReg0;
                                            arg4 = fiveFourZeroImm;
                                            result.append(toFormat(arg0, arg1, arg2, arg3, arg4));
                                        }
                                        break;
                                    case "01":
                                        if (!fiveFourZeroImm.equals("00000")) {
                                            arg2 = "C.SRAI64";
                                            arg3 = destReg0;
                                            result.append(toFormat(arg0, arg1, arg2, arg3));
                                        } else {
                                            arg2 = "C.SRAI";
                                            arg3 = destReg0;
                                            arg4 = fiveFourZeroImm;
                                            result.append(toFormat(arg0, arg1, arg2, arg3, arg4));
                                        }
                                        break;
                                    case "10":
                                        arg2 = "C.ANDI";
                                        arg3 = destReg0;
                                        arg4 = fiveFourZeroImm;
                                        result.append(toFormat(arg0, arg1, arg2, arg3, arg4));
                                        break;
                                    case "11":
                                        String subSubCode = code.substring(9, 11);
                                        arg3 = destReg0;
                                        arg4 = srcReg;
                                        if (code.charAt(3) == '0') {
                                            switch (subSubCode) {
                                                case "00":
                                                    arg2 = "C.SUB";
                                                    break;
                                                case "01":
                                                    arg2 = "C.XOR";
                                                    break;
                                                case "10":
                                                    arg2 = "C.OR";
                                                    break;
                                                case "11":
                                                    arg2 = "C.AND";
                                                    break;
                                            }
                                            result.append(toFormat(arg0, arg1, arg2, arg3, arg4));
                                        } else {
                                            switch (subSubCode) {
                                                case "00":
                                                    arg2 = "C.SUBW";
                                                    result.append(toFormat(arg0, arg1, arg2, arg3, arg4));
                                                    break;
                                                case "01":
                                                    arg2 = "C.ADDW";
                                                    result.append(toFormat(arg0, arg1, arg2, arg3, arg4));
                                                    break;
                                                default:
                                                    arg2 = "unknown_command";
                                                    result.append(toFormat(arg0, arg1, arg2));
                                                    break;
                                            }
                                        }
                                        break;
                                }
                                break;
                            case "101":
                                arg2 = "C.J";
                                int lc = source.fromBin(
                                        number
                                );
                                arg3 = String.valueOf(lc);
                                source.addSym(address + lc - 4);
                                result.append(toJumpFormat(arg0, arg1, arg2, arg3, address + lc - 4));
                                break;
                            case "110":
                                arg2 = "C.BEQZ";
                                arg3 = cRegisters.get(code.substring(6, 9));
                                int lb = source.fromBin(
                                        number1
                                );
                                arg4 = String.valueOf(lb);
                                source.addSym(address + lb - 4);
                                result.append(toJumpFormat(arg0, arg1, arg2, arg3, arg4, address + lb - 4));
                                break;
                            case "111":
                                arg2 = "C.BNEZ";
                                arg3 = cRegisters.get(code.substring(6, 9));
                                int ld = source.fromBin(
                                        number1
                                );
                                arg4 = String.valueOf(ld);
                                source.addSym(ld + address - 4);
                                result.append(toJumpFormat(arg0, arg1, arg2, arg3, arg4, ld + address - 4));
                                break;
                        }
                        break;
                    case "10":
                        part0 = code.substring(0, 3);
                        arg3 = getRegister(code.substring(4, 9));
                        String number2 = "" + code.substring(12, 14) +
                                code.charAt(3) + code.substring(9, 12) + "00";
                        String number3 = "" + code.substring(7, 9) +
                                code.substring(3, 7) + "00";
                        switch (part0) {
                            case "000":
                                if (code.charAt(3) == '0' && code.startsWith("00000", 9)) {
                                    arg2 = "C.SLLI64";
                                    result.append(toFormat(arg0, arg1, arg2, arg3));
                                } else {
                                    arg2 = "C.SLLI";
                                    arg4 = String.valueOf(source.fromBin(
                                            "" + code.charAt(0) + code.substring(9, 14)
                                    ));
                                    result.append(toFormat(arg0, arg1, arg2, arg3, arg4));
                                }
                                break;
                            case "001":
                                arg2 = "C.FLDSP";
                                arg4 = String.valueOf(source.fromAbsBin(
                                        "" + code.substring(11, 14) +
                                                code.charAt(3) + code.substring(9, 11) + "000"
                                ));
                                result.append(toFormat(arg0, arg1, arg2, arg3, arg4));
                                break;
                            case "010":
                                arg2 = "C.LWSP";
                                arg4 = String.valueOf(source.fromAbsBin(
                                        number2
                                ));
                                result.append(lsToFormat(arg0, arg1, arg2, arg3, arg4, "sp"));
                                break;
                            case "011":
                                arg2 = "C.FLWSP";
                                arg4 = String.valueOf(source.fromAbsBin(
                                        number2
                                ));
                                result.append(lsToFormat(arg0, arg1, arg2, arg3, arg4, "sp"));
                                break;
                            case "100":
                                if (code.charAt(3) == '0') {
                                    if (code.startsWith("00000", 9)) {
                                        arg2 = "C.JR";
                                        arg3 = getRegister(code.substring(4, 9));
                                        result.append(toFormat(arg0, arg1, arg2, arg3));
                                    } else {
                                        arg2 = "C.MV";
                                        arg3 = getRegister(code.substring(4, 9));
                                        arg4 = getRegister(code.substring(9, 14));
                                        result.append(toFormat(arg0, arg1, arg2, arg3, arg4));
                                    }
                                } else {
                                    if (code.startsWith("00000", 9)) {
                                        if (code.startsWith("00000", 4)) {
                                            arg2 = "C.EBREAK";
                                            result.append(toFormat(arg0, arg1, arg2));
                                        } else {
                                            arg2 = "C.JALR";
                                            arg3 = getRegister(code.substring(4, 9));
                                            result.append(toFormat(arg0, arg1, arg2, arg3));
                                        }
                                    } else {
                                        arg2 = "C.ADD";
                                        arg3 = getRegister(code.substring(4, 9));
                                        arg4 = getRegister(code.substring(9, 14));
                                        result.append(toFormat(arg0, arg1, arg2, arg3, arg4));
                                    }
                                }
                                break;
                            case "101":
                                arg2 = "C.FSDSP";
                                arg3 = getRegister(code.substring(9, 14));
                                arg4 = String.valueOf(source.fromAbsBin(
                                        "" + code.substring(6, 9) +
                                                code.substring(3, 6) + "000"
                                ));
                                result.append(lsToFormat(arg0, arg1, arg2, arg3, arg4, "sp"));
                                break;
                            case "110":
                                arg2 = "C.SWSP";
                                arg3 = getRegister(code.substring(9, 14));
                                arg4 = String.valueOf(source.fromAbsBin(
                                        number3
                                ));
                                result.append(lsToFormat(arg0, arg1, arg2, arg3, arg4,"sp"));
                                break;
                            case "111":
                                arg2 = "C.FSWSP";
                                arg3 = getRegister(code.substring(9, 14));
                                arg4 = String.valueOf(source.fromAbsBin(
                                        number3
                                ));
                                result.append(lsToFormat(arg0, arg1, arg2, arg3, arg4, "sp"));
                                break;
                        }
                        break;
                }
                address -= 2;
            } else {
                int arg0;
                String arg1;
                String arg2;
                String arg3;
                String arg4;
                String arg5;
                String temp = code.substring(25);
                switch (temp) {
                    case "0110011":
                        result.append(decodeRFormat(code));
                        break;
                    case "0010011":
                        result.append(decodeIFormat(code));
                        break;
                    case "0000011":
                        result.append(decodeLoadFormat(code));
                        break;
                    case "0100011":
                        result.append(decodeSFormat(code));
                        break;
                    case "1100011":
                        result.append(decodeSBFormat(code));
                        break;
                    case "0110111":
                        arg0 = address;
                        arg1 = addSym();
                        arg2 = "LUI";
                        arg3 = getRegister(code.substring(20, 25));
                        arg4 = String.valueOf(source.fromBin("" + code.substring(0, 20)));
                        result.append(toFormat(arg0, arg1, arg2, arg3, arg4));
                        break;
                    case "0010111":
                        arg0 = address;
                        arg1 = addSym();
                        arg2 = "AUIPC";
                        arg3 = getRegister(code.substring(20, 25));
                        arg4 = String.valueOf(source.fromBin("" + code.substring(0, 20)));
                        result.append(toFormat(arg0, arg1, arg2, arg3, arg4));
                        break;
                    case "1101111":
                        arg0 = address;
                        arg1 = addSym();
                        arg2 = "JAL";
                        arg3 = getRegister(code.substring(20, 25));
                        int lb = source.fromBin("" + code.charAt(0) +
                                code.substring(12, 20) + code.charAt(11) + code.substring(1, 11) + "0");
                        arg4 = String.valueOf(lb);
                        source.addSym(lb + address - 4);
                        result.append(toJumpFormat(arg0, arg1, arg2, arg3, arg4, lb + address - 4));
                        break;
                    case "1100111":
                        arg0 = address;
                        arg1 = addSym();
                        arg2 = "JALR";
                        arg3 = getRegister(code.substring(20, 25));
                        arg4 = getRegister(code.substring(12, 17));
                        arg5 = String.valueOf(source.fromBin("" + code.substring(0, 12)));
                        result.append(lsToFormat(arg0, arg1, arg2, arg3, arg5, arg4));
                        break;
                    case "1110011":
                        String part0 = code.substring(0, 12);
                        String part1 = code.substring(12, 17);
                        String part2 = code.substring(17, 20);
                        String part3 = code.substring(20, 25);
                        arg0 = address;
                        arg1 = addSym();
                        if (part2.equals("000")) {
                            if (part0.equals("000000000000")) {
                                arg2 = "ECALL";
                            } else {
                                arg2 = "EBREAK";
                            }
                            result.append(toFormat(arg0, arg1, arg2));
                        } else {
                            arg3 = getRegister(part3);
                            arg5 = systemRegisters.get(part0);
                            if (part2.charAt(0) == '0') {
                                arg4 = getRegister(part1);
                                switch (part2) {
                                    case "001":
                                        arg2 = "CSRRW";
                                        break;
                                    case "010":
                                        arg2 = "CSRRS";
                                        break;
                                    case "011":
                                        arg2 = "CSRRC";
                                        break;
                                    default:
                                        arg2 = "";
                                        break;
                                }
                            }
                            else {
                                arg4 = String.valueOf(source.fromBin(part1));
                                switch (part2) {
                                    case "101":
                                        arg2 = "CSRRWI";
                                        break;
                                    case "110":
                                        arg2 = "CSRRSI";
                                        break;
                                    case "111":
                                        arg2 = "CSRRCI";
                                        break;
                                    default:
                                        arg2 = "";
                                        break;
                                }
                            }
                            result.append(toFormat(arg0, arg1, arg2, arg3, arg4, arg5));
                        }
                        break;
                    default:
                        arg0 = address;
                        arg1 = addSym();
                        arg2 = "unknown_command";
                        result.append(toFormat(arg0, arg1, arg2));
                        break;
                }
            }
        }
        return result;
    }

    private String decodeLoadFormat(String code) {
        String rs1 = code.substring(12, 17);
        int arg0 = address;
        String arg1 = addSym();
        String arg2 = loadCommands.get(code.substring(17, 20));
        if (arg2 == null) {
            arg0 = address;
            arg1 = addSym();
            arg2 = "unknown_command";
            return(toFormat(arg0, arg1, arg2));
        }
        String arg3 = getRegister(code.substring(20, 25));
        String arg4 = String.valueOf(source.fromBin(code.substring(0, 12)));
        String arg5 = getRegister(rs1);
        return lsToFormat(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    private String decodeSBFormat(String code) {
        int arg0 = address;
        String arg1 = addSym();
        String arg2 = sbCommands.get(code.substring(17, 20));
        if (arg2 == null) {
            arg0 = address;
            arg1 = addSym();
            arg2 = "unknown_command";
            return(toFormat(arg0, arg1, arg2));
        }
        String arg3 = getRegister(code.substring(12, 17));
        String arg4 = getRegister(code.substring(7, 12));
        int la = source.fromBin("" + code.charAt(0) + code.charAt(24) +
                code.substring(1, 7) + code.substring(20, 24) + "0");
        String arg5 = String.valueOf(la);
        source.addSym(address + la - 4);
        return toJumpFormat(arg0, arg1, arg2, arg3, arg4, arg5, address + la - 4);
    }

    private String decodeSFormat(String code) {
        int arg0 = address;
        String arg1 = addSym();
        String arg2 = sCommands.get(code.substring(17, 20));
        if (arg2 == null) {
            arg0 = address;
            arg1 = addSym();
            arg2 = "unknown_command";
            return(toFormat(arg0, arg1, arg2));
        }
        String arg5 = getRegister(code.substring(12, 17));
        String arg3 = getRegister(code.substring(7, 12));
        String arg4 = String.valueOf(source.fromBin("" + code.substring(0, 7) + code.substring(20, 25)));
        return lsToFormat(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    private String decodeIFormat(String code) {
        int arg0 = address;
        String arg1 = addSym();
        String arg2, arg3, arg4, arg5;
        String func = code.substring(17, 20);
        if (func.equals("101") || func.equals("001")) {
            if (func.equals("001")) {
                arg2 = "SLLI";
            }
            else if (code.charAt(1) == '1') {
                arg2 = "SRAI";
            } else {
                arg2 = "SRLI";
            }
            arg3 = getRegister(code.substring(20, 25));
            arg4 = getRegister(code.substring(12, 17));
            arg5 = String.valueOf(source.fromBin(code.substring(7, 12)));
        } else {
            arg2 = iCommands.get(func);
            if (arg2 == null) {
                arg0 = address;
                arg1 = addSym();
                arg2 = "unknown_command";
                return(toFormat(arg0, arg1, arg2));
            }
            arg3 = getRegister(code.substring(20, 25));
            arg4 = getRegister(code.substring(12, 17));
            arg5 = String.valueOf(source.fromBin(code.substring(0, 12)));
        }
        return toFormat(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    private String decodeRFormat(String code) {
        int arg0 = address;
        String arg1 = addSym();
        String arg2 = rCommands.get(code.substring(0, 7) + code.substring(17, 20));
        if (arg2 == null) {
            arg2 = "unknown_command";
            return(toFormat(arg0, arg1, arg2));
        }
        String arg3 = getRegister(code.substring(20, 25));
        String arg4 = getRegister(code.substring(12, 17));
        String arg5 = getRegister(code.substring(7, 12));
        return toFormat(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    private String getRegister(String substring) {
        return registers[source.fromAbsBin(substring)];
    }

    private String addSym() {
        StringBuilder result = new StringBuilder();
        String sym = source.getSym(address);
        address += 4;
        if (sym != null) {
            result.append(sym);
        }
        return result.toString();
    }

    private String toJumpFormat(int address, String loc, String command, String arg1, String arg2, int jLoc) {
        if (loc.isEmpty()) {
            return String.format("%08x %10s %s %s, %s %s\n", address, loc, command, arg1, arg2, source.getSym(jLoc));
        }
        return String.format("%08x %10s: %s %s, %s %s\n", address, loc, command, arg1, arg2, source.getSym(jLoc));
    }

    private String toJumpFormat(int address, String loc, String command, String arg1, int jLoc) {
        if (loc.isEmpty()) {
            return String.format("%08x %10s %s %s %s\n", address, loc, command, arg1, source.getSym(jLoc));
        }
        return String.format("%08x %10s: %s %s %s\n", address, loc, command, arg1, source.getSym(jLoc));
    }

    private String toJumpFormat(int address, String loc, String command,
                                String arg1, String arg2, String arg3, int jLoc) {
        if (loc.isEmpty()) {
            return String.format("%08x %10s %s %s, %s, %s %s\n", address,
                    loc, command, arg1, arg2, arg3, source.getSym(jLoc));
        }
        return String.format("%08x %10s: %s %s, %s, %s %s\n", address, loc,
                command, arg1, arg2, arg3, source.getSym(jLoc));
    }

    private String toFormat(int address, String loc, String command, String arg1) {
        if (loc.isEmpty()) {
            return String.format("%08x %10s %s %s\n", address, loc, command, arg1);
        }
        return String.format("%08x %10s: %s %s\n", address, loc, command, arg1);
    }

    private String toFormat(int address, String loc, String command, String arg1, String arg2) {
        if (loc.isEmpty()) {
            return String.format("%08x %10s %s %s, %s\n", address, loc, command, arg1, arg2);
        }
        return String.format("%08x %10s: %s %s, %s\n", address, loc, command, arg1, arg2);
    }

    private String toFormat(int address, String loc, String command, String arg1, String arg2, String arg3) {
        if (loc.isEmpty()) {
            return String.format("%08x %10s %s %s, %s, %s\n", address, loc, command, arg1, arg2, arg3);
        }
        return String.format("%08x %10s: %s %s, %s, %s\n", address, loc, command, arg1, arg2, arg3);
    }

    private String toFormat(int address, String loc, String command) {
        if (loc.isEmpty()) {
            return String.format("%08x %10s %s\n", address, loc, command);
        }
        return String.format("%08x %10s: %s\n", address, loc, command);
    }

    private String lsToFormat(int address, String loc, String command, String arg1, String arg2, String arg3) {
        if (loc.isEmpty()) {
            return String.format("%08x %10s %s %s, %s(%s)\n", address, loc, command, arg1, arg2, arg3);
        }
        return String.format("%08x %10s: %s %s, %s(%s)\n", address, loc, command, arg1, arg2, arg3);
    }
}
