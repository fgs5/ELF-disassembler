import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ELFParser {
    private final HashMap<Integer, String> locs = new HashMap<>();
    private final StringBuilder allLocs = new StringBuilder();
    private final HashMap<String, String> indexes = new HashMap<>(Map.of(
            "0000000000000000", "UNDEF",
            "1111111100000000", "LOPROC",
            "1111111100000001", "AFTER",
            "1111111100011111", "HIPROC",
            "1111111100100000", "LOOS",
            "1111111100111111", "HIOS",
            "1111111111110001", "ABS",
            "1111111111110010", "COMMON",
            "1111111111111111", "XINDEX"
    ));

    private final String[] vises = {"DEFAULT", "INTERNAL", "HIDDEN", "PROTECTED"};

    private final HashMap<Integer, String> types = new HashMap<>(Map.of(
            0, "NOTYPE",
            1, "OBJECT",
            2, "FUNC",
            3, "SECTION",
            4, "FILE",
            5, "COMMON",
            6, "TLS",
            10, "LOOS",
            12, "HIOS",
            13, "SPARC_REGISTER"
    ));

    private final HashMap<Integer, String> binds = new HashMap<>(Map.of(
            0, "LOCAL",
            1, "GLOBAL",
            2, "WEAK",
            10, "LOOS",
            11, "OS",
            12, "HIOS",
            13, "LOPROC",
            14, "PROC",
            15, "HIPROC"
    ));

    private final byte[] source;

    private long textOffset;
    private long textAddress;
    private long textSize;

    private long nameSectionOffset;

    private long symOffset;
    private long symSize;

    private long strOffset;
    private long strSize;

    protected long getTextAddress() {
        return textAddress;
    }

    public ELFParser(Path inputName) throws IOException {
        this.source = Files.readAllBytes(inputName);
    }

    public List<String> parseELF() {
        types.put(11, "OC");
        types.put(14, "PROC");
        types.put(15, "HIPROC");
        for (int i = 61440 + 3840; i < 61440 + 3840 + 16 + 15; i++) {
            String temp = toBin(i);
            if (!indexes.containsKey(temp)) {
                indexes.put(temp, "PROC");
            }
        }
        for (int i = 61440 + 3840 + 320; i < 61440 + 3840 + 480 + 15; i++) {
            String temp = toBin(i);
            if (!indexes.containsKey(temp)) {
                indexes.put(temp, "OS");
            }
        }
        parseHeader();
        parseSymTable();
        return parseData();
    }

    private void parseHeader() {
        if (source[0] != Byte.decode("0x7f") ||
                source[1] != Byte.decode("0x45") ||
                source[2] != Byte.decode("0x4C") ||
                source[3] != Byte.decode("0x46")
        ) {
            throw new AssertionError("Given file is not an .elf file.");
        }
        if (source[4] != Byte.decode("0x1")) {
            throw new AssertionError("Given file is not for x32 system.");
        }
        if (source[5] != Byte.decode("0x1")) {
            throw new AssertionError("Given file is not in little endian.");
        }
        if (source[46] + source[47] * 16 != 40) {
            throw new AssertionError("Section header table is not presented in given file.");
        }
        String b0 = getFormatByte(source[32]);
        String b1 = getFormatByte(source[33]);
        String b2 = getFormatByte(source[34]);
        String b3 = getFormatByte(source[35]);
        int eShoff = fromBin("" + b3 + b2 + b1 + b0);

        b0 = getFormatByte(source[48]);
        b1 = getFormatByte(source[49]);
        b2 = "0000";
        b3 = "0000";
        int eShnum = fromBin("" + b3 + b2 + b1 + b0);

        b0 = getFormatByte(source[50]);
        b1 = getFormatByte(source[51]);
        b2 = "0000";
        b3 = "0000";
        int eShstrndx = fromBin("" + b3 + b2 + b1 + b0);

        b0 = getFormatByte(source[eShoff + (eShstrndx) * 40 + 16]);
        b1 = getFormatByte(source[eShoff + (eShstrndx) * 40 + 17]);
        b2 = getFormatByte(source[eShoff + (eShstrndx) * 40 + 18]);
        b3 = getFormatByte(source[eShoff + (eShstrndx) * 40 + 19]);
        nameSectionOffset = fromBin("" + b3 + b2 + b1 + b0);

        if (eShoff + eShnum * 40 > source.length) {
            throw new AssertionError("File header is not correct.");
        }

        boolean textFlag = false;
        boolean symTableFlag = false;
        boolean strTableFlag = false;

        for (int i = 0; i < eShnum; i++) {
            b0 = getFormatByte(source[eShoff + i * 40]);
            b1 = getFormatByte(source[eShoff + i * 40 + 1]);
            b2 = getFormatByte(source[eShoff + i * 40 + 2]);
            b3 = getFormatByte(source[eShoff + i * 40 + 3]);

            long name = fromBin("" + b3 + b2 + b1 + b0);

            b0 = getFormatByte(source[eShoff + i * 40 + 4]);
            b1 = getFormatByte(source[eShoff + i * 40 + 5]);
            b2 = getFormatByte(source[eShoff + i * 40 + 6]);
            b3 = getFormatByte(source[eShoff + i * 40 + 7]);

            long type = fromBin("" + b3 + b2 + b1 + b0);

            b0 = getFormatByte(source[eShoff + i * 40 + 12]);
            b1 = getFormatByte(source[eShoff + i * 40 + 13]);
            b2 = getFormatByte(source[eShoff + i * 40 + 14]);
            b3 = getFormatByte(source[eShoff + i * 40 + 15]);

            long address = fromBin("" + b3 + b2 + b1 + b0);

            b0 = getFormatByte(source[eShoff + i * 40 + 16]);
            b1 = getFormatByte(source[eShoff + i * 40 + 17]);
            b2 = getFormatByte(source[eShoff + i * 40 + 18]);
            b3 = getFormatByte(source[eShoff + i * 40 + 19]);

            long offset = fromBin("" + b3 + b2 + b1 + b0);

            b0 = getFormatByte(source[eShoff + i * 40 + 20]);
            b1 = getFormatByte(source[eShoff + i * 40 + 21]);
            b2 = getFormatByte(source[eShoff + i * 40 + 22]);
            b3 = getFormatByte(source[eShoff + i * 40 + 23]);

            long size = fromBin("" + b3 + b2 + b1 + b0);

            String tName = getSectionName((int) name);

            if (tName.equals(".text") && type == Byte.decode("0x1")) {
                if (textFlag) {
                    throw new AssertionError("Given file contains 2 .text sections.");
                }
                textAddress = address;
                textOffset = offset;
                textSize = size;
                textFlag = true;
            }
            if (tName.equals(".symtab") && type == Byte.decode("0x2")) {
                if (symTableFlag) {
                    throw new AssertionError("Given file contains 2 .symTable sections.");
                }
                symOffset = offset;
                symSize = size;
                symTableFlag = true;
            }
            if (tName.equals(".strtab") && type == Byte.decode("0x3")) {
                if (strTableFlag) {
                    throw new AssertionError("Given file contains 2 .strTable sections.");
                }
                strOffset = offset;
                strSize = size;
                strTableFlag = true;
            }
        }

        if (!textFlag) {
            throw new AssertionError("Given file does not contain .text section.");
        }
        if (!symTableFlag) {
            throw new AssertionError("Given file does not contain .sym section.");
        }
        if (!strTableFlag) {
            throw new AssertionError("Given file does not contain .str section.");
        }
    }

    private void parseSymTable() {
        int c = 0;
        int count = 0;
        for (long i = symOffset; i < symOffset + symSize; i += 16) {
            String b0 = getFormatByte(source[(int) (i)]);
            String b1 = getFormatByte(source[(int) (i + 1)]);
            String b2 = getFormatByte(source[(int) (i + 2)]);
            String b3 = getFormatByte(source[(int) (i + 3)]);
            long name = fromBin("" + b3 + b2 + b1 + b0);

            String b4 = getFormatByte(source[(int) (i + 4)]);
            String b5 = getFormatByte(source[(int) (i + 5)]);
            String b6 = getFormatByte(source[(int) (i + 6)]);
            String b7 = getFormatByte(source[(int) (i + 7)]);
            long address = fromBin("" + b7 + b6 + b5 + b4);

            String b8 = getFormatByte(source[(int) (i + 8)]);
            String b9 = getFormatByte(source[(int) (i + 9)]);
            String b10 = getFormatByte(source[(int) (i + 10)]);
            String b11 = getFormatByte(source[(int) (i + 11)]);
            long size = fromBin("" + b11 + b10 + b9 + b8);

            String type = types.get(source[(int) i + 12] % 16);
            if (type == null) {
                type = "UNKNOWN";
            }

            String bind = binds.get(source[(int) i + 12] / 16);
            if (bind == null) {
                bind = "UNKNOWN";
            }

            String vis;
            if (source[(int) i + 13] % 16 >= vises.length) {
                vis = "UNKNOWN";
            } else {
                vis = vises[source[(int) i + 13] % 16];
            }

            String b14 = getFormatByte(source[(int) (i + 14)]);
            String b15 = getFormatByte(source[(int) (i + 15)]);
            String index;
            String tempIndex = indexes.get("" + b15 + b14);

            if (tempIndex != null) {
                index = tempIndex;
            } else {
                index = String.valueOf(fromBin("" + b15 + b14));
            }

            String resName = "";
            if (name == 0 && type.equals("FUNC")) {
                locs.merge((int) (address), " " + String.format("LOC_%05x", c), String::concat);
                c++;
            } else {
                StringBuilder temp = new StringBuilder();
                for (long j = strOffset + name; j < strOffset + strSize; j++) {
                    if (source[(int) j] == 0) {
                        if (type.equals("FUNC")) {
                            locs.merge((int) (address), " " + temp.toString(), String::concat);
                        }
                        resName = temp.toString();
                        break;
                    }
                    temp.append(new String(new byte[] {source[(int) j]}));
                }
                if (resName.equals("")) {
                    resName = temp.toString();
                    if (type.equals("FUNC")) {
                        locs.merge((int) (address), temp.toString(), String::concat);
                    }
                }
            }
            allLocs.append(String.format("[%4x] 0x%-15x %5d %-8s %-8s %-8s %6s %s\n",
                    count, address, size, type, bind, vis, index, resName));
            count++;
        }
    }

    private List<String> parseData() {
        List<String> result = new ArrayList<>();
        for (long i = textOffset; i < textOffset + textSize; i += 2) {
            StringBuilder temp = new StringBuilder();
            String b0 = getFormatByte(source[(int) (i)]);
            String b1 = getFormatByte(source[(int) (i + 1)]);
            if (b0.substring(6).equals("11")) {
                String b2 = getFormatByte(source[(int) (i + 2)]);
                String b3 = getFormatByte(source[(int) (i + 3)]);
                temp.append(b3).append(b2).append(b1).append(b0);
                i += 2;
            } else {
                temp.append(b1).append(b0);
            }
            result.add(temp.toString());
        }
        return result;
    }

    protected int fromBin(String number) {
        int result;
        if (number.charAt(0) == '1') {
            StringBuilder temp = new StringBuilder();
            for (int i = 0; i < number.length(); i++) {
                if (number.charAt(i) == '0') {
                    temp.append('1');
                } else {
                    temp.append('0');
                }
            }
            result = Integer.parseUnsignedInt(temp.toString(), 2);
            result++;
            result = -result;
        } else {
            result = Integer.parseUnsignedInt(number, 2);
        }
        return result;
    }

    protected int fromAbsBin(String number) {
        int result = 0;
        int pow = 1;
        for (int i = number.length() - 1; i > -1; i--) {
            if (number.charAt(i) == '1') {
                result += pow;
            }
            pow *= 2;
        }
        return result;
    }

    protected String getSym(int address) {
        return locs.get(address);
    }

    protected void addSym(int address) {
        if (!locs.containsKey(address)) {
            locs.put(address, String.format("LOC_%05x", address));
        }
    }

    protected String getAllLocs() {
        return allLocs.toString();
    }

    protected String toBin(int n) {
        StringBuilder result = new StringBuilder();
        while (n > 0) {
            result.insert(0, n % 2);
            n /= 2;
        }
        while (result.length() < 12) {
            result.insert(0, "0");
        }
        return result.toString();
    }

    protected String getSectionName(int offset) {
        long iter = offset + nameSectionOffset;
        StringBuilder temp = new StringBuilder();
        while (source[(int) iter] != 0 || (iter == offset + nameSectionOffset)) {
            temp.append(new String(new byte[] {source[(int) iter]}));
            iter++;
        }
        return temp.toString();
    }

    protected String getFormatByte(byte src) {
        return String.format("%8s", Integer.toBinaryString(src & 0xFF)).replace(' ', '0');
    }
}
