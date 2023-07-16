import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public class CMParser {
    private static byte[] buff = new byte[1024];

    private record vertex(float x, float y, float z) {};

    public static void main(String[] args) throws Exception {
        try (RandomAccessFile file = new RandomAccessFile("C:\\utils\\xbox\\_mars_city1_1.gob\\base\\xbox_gen\\maps\\vv\\mars_city1_1.cm.x", "r")) {
//            file.seek(Integer.parseInt("166a17", 16));
//            file.seek(Integer.parseInt("166a38", 16));
//            file.seek(Integer.parseInt("16de52", 16));
//            file.seek(Integer.parseInt("16de35", 16));
            file.seek(Integer.parseInt("16de20", 16));
            String name = readString(file);
            System.out.println(name);
            vertex v1 = readVertex(file);
            vertex v2 = readVertex(file);
            System.out.println("( " + v1.x + " " + v1.y + " " + v1.z + " ) ");
            System.out.println("( " + v2.x + " " + v2.y + " " + v2.z + " ) ");
            int type = readInt(file);
            System.out.println(" type " + type);
            readUnsignedByte(file);
            if (type == 3) readVerts(file);
            System.out.format("%08X\n", file.getFilePointer());
        }
    }

    private static void readVerts(RandomAccessFile file) throws Exception {
        int numVerts = readInt(file);
        for (int i=0; i<numVerts; i++) {
            vertex v = readVertex(file);
            System.out.println("( " + v.x + " " + v.y + " " + v.z + " ) ");
        }
    }

    private static String readString(RandomAccessFile file) throws Exception {
        int num = readInt(file);
        readData(file, buff, num);
        return new String(buff, 0, num, StandardCharsets.US_ASCII);
    }

    private static vertex readVertex(RandomAccessFile file) throws Exception {
        return new vertex(readFloat(file),readFloat(file), readFloat(file));
    }

    private static float readFloat(RandomAccessFile file) throws Exception {
        int i = readInt(file);
        return Float.intBitsToFloat(i);
    }

    private static int readUnsignedByte(RandomAccessFile file) throws Exception {
        readData(file, buff, 1);
        int data = (buff[0] & 0xFF);
        return data;
    }

    private static int readInt(RandomAccessFile file) throws Exception {
        readData(file, buff, 4);
        int data = (buff[0] & 0xFF) + ((buff[1] & 0xFF) << 8) + ((buff[2] & 0xFF) << 16) + ((buff[3] & 0xFF) << 24);
        return data;
    }

    private static void readData(RandomAccessFile file, byte[] data, int toBeRead) throws Exception {
        file.readFully(data, 0,toBeRead);
    }
}
