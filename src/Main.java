import java.io.*;
import java.nio.file.FileSystems;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Please, insert names of the input/output files.");
            System.exit(0);
        }
        try {
            ELFParser source = new ELFParser(FileSystems.getDefault().getPath(args[0]));
            List<String> codes = source.parseELF();
            StringBuilder result = new RISCV2Assembler(codes, source).convert();
            try (BufferedWriter out = new BufferedWriter(new FileWriter(new File(args[1])))) {
                out.write(".text");
                out.write(System.lineSeparator());
                out.write(result.toString());
                out.write(System.lineSeparator());
                out.write(".symtab");
                out.write(System.lineSeparator());
                out.write(String.format("%s %-15s %7s %-8s %-8s %-8s %6s %s\n",
                        "Symbol", "Value", "Size", "Type", "Bind", "Vis", "Index", "Name"));
                out.write(source.getAllLocs());
            } catch (IOException e) {
                System.out.println("Can't write in a file " + args[1] + ": " + e.getMessage());
            }
        } catch (AssertionError e) {
            System.out.println("Incorrect input: " + e.getMessage());
            System.out.println(e.getMessage());
        } catch (FileNotFoundException e) {
            System.out.println("No such file " + args[0] + ": " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Can't read from file " + args[0] + ": " + e.getMessage());
        }
    }
}
