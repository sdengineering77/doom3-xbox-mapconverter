import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public abstract class ConverterBase {

    protected byte[] buff = new byte[1024];
    protected StringBuilder b = new StringBuilder();
    protected String currentToken;
    protected long rawTokenLength;
    protected int indent = 0;

    protected DecimalFormat df;

    protected RandomAccessFile input;

    protected ConverterBase(RandomAccessFile file) throws Exception {
        this.input = file;
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.getDefault());
        dfs.setDecimalSeparator('.');
        df = new DecimalFormat("0.#########", dfs);
    }

    abstract void convert() throws Exception;

    protected boolean isEOF() throws Exception {
        return (input.getFilePointer() >= input.length());
    }

    protected String[] readTokenPair() throws Exception {
        return new String[] {readToken(),  readToken()};
    }

    protected void unreadToken() throws Exception {
        input.seek(input.getFilePointer() - rawTokenLength);
        currentToken = null;
        rawTokenLength = 0;
    }


    protected String readToken() throws Exception {
        boolean parsing = false;
        boolean quoted = false;
        b.setLength(0);
        byte read;
        while((read = input.readByte()) > 0x20 || !parsing || quoted) {
            if (read > 0x20) parsing = true;
            if (parsing && read == '"') quoted = !quoted;
            if (parsing) {
                b.append((char) read);
            }
        }
        currentToken = b.toString();
        rawTokenLength = currentToken.length() + 1;
        return currentToken;
    }

    protected float[] load1DMatrix(int length, float[] result) throws Exception {
        for (int i=0; i<length; i++) {
            result[i] = readFloat();
        }
        return result;
    }

    protected void patternSync(String pattern) throws Exception {
        for (byte lookFor : pattern.getBytes(StandardCharsets.US_ASCII)) {
            while(input.readByte() != lookFor);
        }
    }

    protected float readFloat() throws Exception {
        int i = readInt();
        return Float.intBitsToFloat(i);
    }

    protected float readFP_0_16_Float() throws Exception {
        readData(2);
        float raw = (buff[0] & 0xFF) + ((buff[1] & 0xFF) << 8);
        float value = (raw - 0x8000) / 0x8000;

        return value;
    }

    protected int readInt() throws Exception {
        readData(4);
        int data = (buff[0] & 0xFF) + ((buff[1] & 0xFF) << 8) + ((buff[2] & 0xFF) << 16) + ((buff[3] & 0xFF) << 24);
        return data;
    }

    protected void readData(int toBeRead) throws Exception {
        input.readFully(buff, 0,toBeRead);
    }

    protected void printNumber(float number) {
        System.out.print(df.format(number));
    }

    protected void println(String... txt) {
        if (txt.length > 0) {
            for (int i=0; i<indent; i++) System.out.print(' ');
            System.out.println(txt[0]);
        } else {
            System.out.println();
        }
    }
    protected void printf(String txt, Object... vars) {
        for (int i=0; i<indent; i++) System.out.print(' ');
        System.out.printf(txt, vars);
    }

    protected void printf_noindent(String txt, Object... vars) {
        System.out.printf(txt, vars);
    }
    
    
}
