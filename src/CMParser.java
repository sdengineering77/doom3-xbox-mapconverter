import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public class CMParser extends ConverterBase {

    private record vertex(float x, float y, float z) {};
    private record plane(float x, float y, float z, float dist) {};

    public static void main(String[] args) throws Exception {
        try (RandomAccessFile file = new RandomAccessFile("C:\\utils\\xbox\\_mars_city1_1.gob\\base\\xbox_gen\\maps\\vv\\mars_city1_1.cm.x", "r")) {
            CMParser p = new CMParser(file);
            p.convert();
        }
    }

    public CMParser(RandomAccessFile file) throws Exception {
        super(file);
    }

    @Override
    void convert() throws Exception {
//            file.seek(Integer.parseInt("166a17", 16));
//            file.seek(Integer.parseInt("166a38", 16));
//            file.seek(Integer.parseInt("16de52", 16));
//        input.seek(Integer.parseInt("16de35", 16));
//        input.seek(Integer.parseInt("16de20", 16));
        input.seek(Integer.parseInt("169853", 16));
        String name = readString(); // the cm name
        System.out.println(name);
        vertex v1 = readVertex(); // really don't know what these two vertices are... Bounding boxes for brushes perhaps?
        vertex v2 = readVertex();
        System.out.println("( " + df.format(v1.x) + " " + df.format(v1.y) + " " + df.format(v1.z) + " ) ");
        System.out.println("( " + df.format(v2.x) + " " + df.format(v2.y) + " " + df.format(v2.z) + " ) ");
        int type = readInt(); // seems to be some kind of type...
        System.out.println(" type " + type);
        readUnsignedByte(); // filler
        if (type == 3) readVerts(); // the vertices, pretty much straightforward
        System.out.format("%08X\n", input.getFilePointer());
        readEdges(); // the edges... do contain more than that
        System.out.format("%08X\n", input.getFilePointer());
        plane[] planes = readPlanes(); // the planes (refered to in polies and brushes?)
        System.out.format("%08X\n", input.getFilePointer());
        readPolies(planes); // the polies and brushes
        System.out.format("%08X\n", input.getFilePointer());

    }

    private plane[] readPlanes() throws Exception {
        int numData = readInt();
        plane[] planes = new plane[numData];
        for (int i=0; i<numData; i++) {
            planes[i] = new plane(readFloat(), readFloat(), readFloat(), readFloat());
            plane plane = planes[i];
            printf("( %s %s %s ) %s \n", df.format(plane.x), df.format(plane.y), df.format(plane.z), df.format(plane.dist));
        }
        return planes;
    }

    private void readPolies(plane[] planes) throws Exception {
        int numToRead;
        while((numToRead = readInt()) > 0) {
            int type = readInt();
            printf("%d %d ", numToRead, type);
            if (type == -1) { // brushes
                readBrushes(24, planes); // where is the counter, must be a value 0x18 somewhere???
                return;
            } else if (numToRead == type) {
                readPolies(numToRead, planes);
            } else {
                printf("Numbers do not equal %06X", input.getFilePointer());
            }
        }
    }

    private void readBrushes(int numToRead, plane[] planes) throws Exception {
        for (int j=0; j<numToRead; j++) {
            int i = readInt();
            printf(" %d ", i);
            plane plane = planes[i];
            printf("( %s %s %s ) %s \n", df.format(plane.x), df.format(plane.y), df.format(plane.z), df.format(plane.dist));
        }
        readWord(); // spacer?
    }
    private void readPolies(int numToRead, plane[] planes) throws Exception {
        for (int c = 0; c < numToRead; c++) { // how to know number of polies?
            int numtbr = readUnsignedByte(); // seems to indicate if there is a filler byte?
            if (numtbr == 0x06) {
                for (int i = 0; i < 3; i++) readUnsignedByte();
            }
            printf(" [%d] ", numtbr); // this may be the number of edges
            for (int i = 0; i < numtbr; i++) {
                printf(" %d ", readInt()); // what is this?
            }
            printf("( %s %s %s ) ", df.format(readFloat()), df.format(readFloat()), df.format(readFloat()));
            printf("( %s %s %s ) ", df.format(readFloat()), df.format(readFloat()), df.format(readFloat()));
            if (numtbr == 6) printf(" %d", readInt()); // what is this?
            int normalsIdx = readInt();
            printf(" %d ( %s %s %s ) %s ", normalsIdx, df.format(planes[normalsIdx].x), df.format(planes[normalsIdx].y), df.format(planes[normalsIdx].z), df.format(planes[normalsIdx].dist));

            println(readString());
        }

    }

    private void readEdges() throws Exception {
        int numEdges = readInt();

        for (int i=0; i<numEdges; i++) {
            printNumber(readWord()); // what is this? alignment?
            int internal = readWord();
            int numUsers = readWord();
            readWord();
            int vertex1 = readWord();
            int vertex2 = readWord();
            printf("( %s %s ) ", df.format(readFP_0_16_Float()), df.format(readFP_0_16_Float())); // so these are something but not sure what yet, probably not fp0.16 though...
            printf("( %d %d ) %d %d\n", vertex1, vertex2, internal, numUsers);
        }

    }

    private void readVerts() throws Exception {
        int numVerts = readInt();
        for (int i=0; i<numVerts; i++) {
            vertex v = readVertex();
            System.out.println("( " + v.x + " " + v.y + " " + v.z + " ) ");
        }
    }

    private String readString() throws Exception {
        int num = readInt();
        readData(num);
        return new String(buff, 0, num, StandardCharsets.US_ASCII);
    }

    private vertex readVertex() throws Exception {
        return new vertex(readFloat(),readFloat(), readFloat());
    }


    private int readUnsignedByte() throws Exception {
        readData(1);
        int data = (buff[0] & 0xFF);
        return data;
    }


}
