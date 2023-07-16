import java.io.File;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public class MapParser {
    private static final String FP_PATTERN_9 = "%.9f";
    private static final String FP_PATTERN_0 = "%.0f";
    private static byte[] buff = new byte[1024];
    private static StringBuilder b = new StringBuilder();
    private static String currentToken;
    private static long rawTokenLength;
    private static int indent = 0;

    public static void main(String[] args) throws Exception {
        try (RandomAccessFile input = new RandomAccessFile("C:\\utils\\xbox\\_mars_city1_1.gob\\base\\xbox_gen\\maps\\vv\\mars_city1_1.map.x", "r")) {
            PrintStream o = System.out;
            File outputDir = new File("c:\\temp\\xbox\\");
            if (!outputDir.exists()) outputDir.mkdirs();
            System.setOut(new PrintStream("c:\\temp\\xbox\\mars_city1_1.map"));

            printf("%s %s\n", readToken(input), readToken(input));
            printMapEntries(input);
            System.out.flush();
            System.out.close();
            System.setOut(o);

        }
    }

    private static void printMapEntries(RandomAccessFile file) throws Exception {
        while(file.getFilePointer() < file.length() && "{".equals(readToken(file))) {
            println("{");
            indent++;
            printMapEntryMeta(file);
            printDefs(file);
            indent--;
            println("}");
        }
    }

    private static void printMapEntryMeta(RandomAccessFile file) throws Exception {
        while(!"{".equals(readToken(file))) {
            printf("%s %s\n", currentToken, readToken(file));
        }
        unreadToken(file);
    }

    private static void printDefs(RandomAccessFile file) throws Exception {
        while("{".equals(readToken(file))) {
            println("{ ");
            String type = readToken(file);
            indent++;
            if ("brushDef3".equals(type)) {
                printBrushDef3(file);
            } else if ("patchDef2".equals(type)) {
                printPatchDef(file, false);
            } else if ("patchDef3".equals(type)) {
                printPatchDef(file, true);
            } else {
                skipBody(file);
            }
            indent--;
            patternSync(file, "}");
            println("} ");
        }

    }

    private static void skipBody(RandomAccessFile file) throws Exception {
        int level = 1;
        while(!"{".equals(readToken(file)));
        while(level > 0) {
            String token = readToken(file);
            if ("{".equals(token)) level++;
            if ("}".equals(token)) level--;

        }
    }

    private static void printBrushDef3(RandomAccessFile file) throws Exception {
        println("brushDef3 { ");
        indent++;
        while(!"{".equals(readToken(file)));
        while(!"}".equals(readToken(file))) {
            printf("");
            printBrushDef3Rec(file);
            println();
        }
        indent--;
        println("} ");
    }

    private static void printPatchDef(RandomAccessFile file, boolean isPatchDef3) throws Exception {
        if (isPatchDef3) {
            println("patchDef3 { ");
        } else {
            println("patchDef2 { ");
        }
        indent++;
        while(!"{".equals(readToken(file)));
        printf("");
        printLexerString(file);
        println();
        while(!"}".equals(readToken(file))) {
            printPatchDefRec(file, isPatchDef3);
        }
        indent--;
        println("} ");
    }

    private static void unreadToken(RandomAccessFile file) throws Exception {
        file.seek(file.getFilePointer() - rawTokenLength);
        currentToken = null;
        rawTokenLength = 0;
    }

    private static String readToken(RandomAccessFile file) throws Exception {
        boolean parsing = false;
        boolean quoted = false;
        b.setLength(0);
        byte read;
        while((read = file.readByte()) > 0x20 || !parsing || quoted) {
            if (read > 0x20) parsing = true;
            if (parsing && read == '"') quoted = quoted ? false : true;
            if (parsing) {
                b.append((char) read);
            }
        }
        currentToken = b.toString();
        rawTokenLength = currentToken.length() + 1;
        return currentToken;
    }

    private static void printPatchDefRec(RandomAccessFile file, boolean isPatchDef3) throws Exception {
        float[] info;
        if (isPatchDef3) {
            info = new float[] {0, 0, 0, 0, 0, 0, 0}; // pc version has 3 more values (usually 0?)
            load1DMatrix(file, 4, info);
        } else {
            info = new float[] {0, 0, 0, 0, 0}; // pc version has 3 more values (usually 0?)
            load1DMatrix(file, 2, info);
        }
        printf("");
        print1DMatrix(info, FP_PATTERN_0);
        int rows = (int) info[0];
        int columns = (int) info[1];
        println();
        println("(");
        indent++;
        for (int r=0; r<rows; r++) {
            printf("( ");
            for (int c=0; c<columns; c++) {
                print1DMatrix(file, 5, FP_PATTERN_9);
            }
            printf_noindent(")\n");
        }
        indent--;
        println(")");
        patternSync(file," #");
    }

    private static void printBrushDef3Rec(RandomAccessFile file) throws Exception {
        print1DMatrix(file, 4, FP_PATTERN_9);
        printf_noindent("( ");
        print1DMatrix(file, 3, FP_PATTERN_9);
        print1DMatrix(file, 3, FP_PATTERN_9);
        printf_noindent(") ");
        patternSync(file,"# ");
        printLexerString(file);
    }

    private static void print1DMatrix(RandomAccessFile file, int length, String pattern) throws Exception {
        float[] values = new float[length];
        load1DMatrix(file, length, values);
        print1DMatrix(values, pattern);
    }

    private static void print1DMatrix(float[] values, String pattern) throws Exception {
        printf_noindent("( ");
        for (int i=0; i<values.length; i++) {
            printf_noindent(pattern + " ", values[i]);
        }
        printf_noindent(") ");

    }

    private static float[] load1DMatrix(RandomAccessFile file, int length, float[] result) throws Exception {
        for (int i=0; i<length; i++) {
            result[i] = readFloat(file);
        }
        return result;
    }

    private static void printLexerString(RandomAccessFile file) throws Exception {
        b.setLength(0);
        patternSync(file, "\"");
        byte read;
        b.append('"');
        while((read = file.readByte()) != '"') {
            b.append((char) read);
        }
        b.append('"');
        printf_noindent(b.toString());
    }

    private static void patternSync(RandomAccessFile file, String pattern) throws Exception {
        for (byte lookFor : pattern.getBytes(StandardCharsets.US_ASCII)) {
            while(file.readByte() != lookFor);
        }
    }

    private static float readFloat(RandomAccessFile file) throws Exception {
        int i = readInt(file);
        return Float.intBitsToFloat(i);
    }

    private static int readInt(RandomAccessFile file) throws Exception {
        readData(file, buff, 4);
        int data = (buff[0] & 0xFF) + ((buff[1] & 0xFF) << 8) + ((buff[2] & 0xFF) << 16) + ((buff[3] & 0xFF) << 24);
        return data;
    }

    private static void readData(RandomAccessFile file, byte[] data, int toBeRead) throws Exception {
        file.readFully(data, 0,toBeRead);
    }

    private static void println(String... txt) {
        if (txt.length > 0) {
            for (int i=0; i<indent; i++) System.out.print(' ');
            System.out.println(txt[0]);
        } else {
            System.out.println();
        }
    }
    private static void printf(String txt, Object... vars) {
        for (int i=0; i<indent; i++) System.out.print(' ');
        System.out.printf(txt, vars);
    }
    
    private static void printf_noindent(String txt, Object... vars) {
        System.out.printf(txt, vars);
    }
}
