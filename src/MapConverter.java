import java.io.PrintStream;
import java.io.RandomAccessFile;

public class MapConverter extends ConverterBase {
    public static void main(String[] args) throws Exception {

        try (RandomAccessFile input = new RandomAccessFile("C:\\utils\\xbox\\_mars_city1_1.gob\\base\\xbox_gen\\maps\\vv\\mars_city1_1.map.x", "r")) {
            PrintStream o = System.out;
//            File outputDir = new File("c:\\temp\\xbox\\");
//            if (!outputDir.exists()) outputDir.mkdirs();
//            System.setOut(new PrintStream("c:\\temp\\xbox\\mars_city1_1.map"));
            
            MapConverter c = new MapConverter(input);
            c.convert();

            System.out.flush();
            System.out.close();
            System.setOut(o);

        }
    }

    public MapConverter(RandomAccessFile file) throws Exception {
        super(file);
    }

    @Override
    void convert() throws Exception {
        printf("%s %s\n", readToken(), readToken());
        printMapEntries();
    }

    private void printMapEntries() throws Exception {
        while(!isEOF() && "{".equals(readToken())) {
            println("{");
            indent++;
            printMapEntryMeta();
            printDefs();
            indent--;
            println("}");
        }
    }

    private void printMapEntryMeta() throws Exception {
        while(!"{".equals(readToken())) {
            printf("%s %s\n", currentToken, readToken());
        }
        unreadToken();
    }

    private void printDefs() throws Exception {
        while("{".equals(readToken())) {
            println("{ ");
            String type = readToken();
            indent++;
            if ("brushDef3".equals(type)) {
                printBrushDef3();
            } else if ("patchDef2".equals(type)) {
                printPatchDef(false);
            } else if ("patchDef3".equals(type)) {
                printPatchDef(true);
            } else {
                skipBody();
            }
            indent--;
            patternSync("}");
            println("} ");
        }

    }

    private void skipBody() throws Exception {
        int level = 1;
        while(!"{".equals(readToken()));
        while(level > 0) {
            String token = readToken();
            if ("{".equals(token)) level++;
            if ("}".equals(token)) level--;

        }
    }

    private void printBrushDef3() throws Exception {
        println("brushDef3");
        println("{");
        indent++;
        while(!"{".equals(readToken()));
        while(!"}".equals(readToken())) {
            printf("");
            printBrushDef3Rec();
            println();
        }
        indent--;
        println("} ");
    }

    private void printPatchDef(boolean isPatchDef3) throws Exception {
        if (isPatchDef3) {
            println("patchDef3");
        } else {
            println("patchDef2");
        }
        println("{");
        indent++;
        while(!"{".equals(readToken()));
        printf("");
        printLexerString();
        println();
        while(!"}".equals(readToken())) {
            printPatchDefRec(isPatchDef3);
        }
        indent--;
        println("} ");
    }

    private void printPatchDefRec(boolean isPatchDef3) throws Exception {
        float[] info;
        if (isPatchDef3) {
            info = new float[] {0, 0, 0, 0, 0, 0, 0}; // pc version has 3 more values (usually 0?)
            load1DMatrix(4, info);
        } else {
            info = new float[] {0, 0, 0, 0, 0}; // pc version has 3 more values (usually 0?)
            load1DMatrix(2, info);
        }
        printf("");
        print1DMatrix(info);
        int rows = (int) info[0];
        int columns = (int) info[1];
        println();
        println("(");
        indent++;
        for (int r=0; r<rows; r++) {
            printf("( ");
            for (int c=0; c<columns; c++) {
                print1DMatrix(5);
            }
            printf_noindent(")\n");
        }
        indent--;
        println(")");
        patternSync(" #");
    }

    private void printBrushDef3Rec() throws Exception {
        print1DMatrix(4);
        printf_noindent("( ");
        print1DMatrix(3);
        print1DMatrix(3);
        printf_noindent(") ");
        patternSync("# ");
        printLexerString();
        printf_noindent(" 0 0 0");
    }

    private void print1DMatrix(int length) throws Exception {
        float[] values = new float[length];
        load1DMatrix(length, values);
        print1DMatrix(values);
    }

    private void print1DMatrix(float[] values) throws Exception {
        printf_noindent("( ");
        for (int i=0; i<values.length; i++) {
            printNumber(values[i]);
            printf_noindent(" ");
        }
        printf_noindent(") ");

    }

    private void printLexerString() throws Exception {
        b.setLength(0);
        patternSync("\"");
        byte read;
        b.append('"');
        while((read = input.readByte()) != '"') {
            b.append((char) read);
        }
        b.append('"');
        printf_noindent(b.toString());
    }

}
