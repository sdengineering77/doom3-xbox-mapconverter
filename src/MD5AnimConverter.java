import java.io.File;
import java.io.PrintStream;
import java.io.RandomAccessFile;

public class MD5AnimConverter extends ConverterBase {

    private static final int ANIM_TX = 1<<0;
    private static final int ANIM_TY = 1<<1;
    private static final int ANIM_TZ = 1<<2;
    private static final int ANIM_QX = 1<<3;
    private static final int ANIM_QY = 1<<4;
    private static final int ANIM_QZ = 1<<5;

    private record JointAnimation(boolean animTx, boolean animTy, boolean animTz, boolean animQx, boolean animQy, boolean animQz) {}
    private record AnimationFrame(int numPositions, int numOrientations, JointAnimation[] animations) {}

    public static void main(String[] args) throws Exception {
        //
//        try (RandomAccessFile input = new RandomAccessFile("C:\\utils\\xbox\\_mars_city1_1.gob\\base\\xbox_gen\\models\\md5\\heads\\sarge\\marscity\\sargecin_wait_loop.md5anim.x", "r")) {
        try (RandomAccessFile input = new RandomAccessFile("C:\\utils\\xbox\\_mars_city1_1.gob\\base\\xbox_gen\\models\\md5\\environments\\storagecabinet.md5anim.x", "r")) {
//        try (RandomAccessFile input = new RandomAccessFile("C:\\utils\\xbox\\_mars_city1_1.gob\\base\\xbox_gen\\models\\md5\\items\\flashlight_view\\swing1.md5anim.x", "r")) {
            PrintStream o = System.out;
            File outputDir = new File("c:\\temp\\xbox\\");
            if (!outputDir.exists()) outputDir.mkdirs();
            System.setOut(new PrintStream("c:\\temp\\xbox\\models\\md5\\environments\\storagecabinet.md5anim.x"));
            MD5AnimConverter c = new MD5AnimConverter(input);
            c.convert();

            System.out.flush();
            System.out.close();
            System.setOut(o);

        }
    }

    public MD5AnimConverter(RandomAccessFile file) throws Exception {
        super(file);
    }


    public void convert() throws Exception {
        int numFrames, numJoints;
        String[] tokenPair;
        // version
        printTokenPair();
        // commandline
        printTokenPair();
        // num Frames
        tokenPair = readTokenPair();
        numFrames = Integer.parseInt(tokenPair[1]);
        printTokenPair(tokenPair);
        // joints
        tokenPair = readTokenPair();
        numJoints = Integer.parseInt(tokenPair[1]);
        printTokenPair(tokenPair);
        // rate
        printTokenPair();
        // animated components
        printTokenPair();
        // hierarchy
        AnimationFrame frame = printAndParseHierarchy(numJoints);
        // bounds
        printBlock();
        // baseframe
        printBlock();
        println();
        printFrames(numFrames, frame);
    }

    private AnimationFrame printAndParseHierarchy(int numJoints) throws Exception {
        int numPositions = 0, numOrientations = 0;
        JointAnimation[] animations = new JointAnimation[numJoints];

        long fp = input.getFilePointer();
        String block = readBlock();
        println(block);
        input.seek(fp);

        patternSync("{");
        for (int i=0; i<numJoints; i++) {
            String animBitsToken;
            // name
            readToken();
            // parent
            readToken();
            // anim
            animBitsToken = readToken();
            patternSync("\n");
            int animBits = Integer.parseInt(animBitsToken);
            animations[i] = createJointAnimation(animBits);
            numPositions += animations[i].animTx ? 1 : 0;
            numPositions += animations[i].animTy ? 1 : 0;
            numPositions += animations[i].animTz ? 1 : 0;
            numOrientations += animations[i].animQx ? 1 : 0;
            numOrientations += animations[i].animQy ? 1 : 0;
            numOrientations += animations[i].animQz ? 1 : 0;

        }
        patternSync("}");

        return new AnimationFrame(numPositions, numOrientations, animations);
    }

    private JointAnimation createJointAnimation(int animBits) {
        return new JointAnimation(
            testBit(animBits, ANIM_TX),
            testBit(animBits, ANIM_TY),
            testBit(animBits, ANIM_TZ),
            testBit(animBits, ANIM_QX),
            testBit(animBits, ANIM_QY),
            testBit(animBits, ANIM_QZ)
        );
    }

    private boolean testBit(int value, int bit) {
        return (value & bit) > 0;
    }
    private void printFrames(int numFrames, AnimationFrame frame) throws Exception {
        patternSync("# ");
        long positionPtr = input.getFilePointer();;
        long orientationPtr = positionPtr + (long) frame.numPositions * numFrames * 4; // sizeof float
        for (int f=0; f<numFrames; f++) {
            printf("frame %d {\n", f);
            indent++;
            for (JointAnimation animation : frame.animations) {
                b.setLength(0);
                input.seek(positionPtr);
                if (animation.animTx) b.append(df.format(readFloat()) + " ");
                if (animation.animTy) b.append(df.format(readFloat()) + " ");
                if (animation.animTz) b.append(df.format(readFloat()) + " ");
                positionPtr = input.getFilePointer();
                input.seek(orientationPtr);
                if (animation.animQx) b.append(df.format(readFP_0_16_Float()) + " ");
                if (animation.animQy) b.append(df.format(readFP_0_16_Float()) + " ");
                if (animation.animQz) b.append(df.format(readFP_0_16_Float()) + " ");
                orientationPtr = input.getFilePointer();
                if (b.length() > 0) {
                    println(b.toString());
                }
            }
            indent--;
            patternSync(" #");
            println("}");
            println();
        }
    }

    private void printTokenPair() throws Exception {
        printf("%s %s\n", (Object[]) readTokenPair());
    }

    private void printTokenPair(String[] pair) throws Exception {
        printf("%s %s\n", (Object[]) pair);
    }

    private void printBlock() throws Exception {
        println(readBlock());
    }

    private String readBlock() throws Exception {
        b.setLength(0);
        byte read;
        while((read = input.readByte()) != '}') {
            b.append((char) read);
        }
        b.append((char) read);
        return b.toString();
    }

}
