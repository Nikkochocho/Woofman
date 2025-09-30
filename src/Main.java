public class Main  {

    private static final String PATH_ORIGINAL = "../Woofman/resources/video.mp4";
    private static final String PATH_ENCODED  = "../Woofman/resources/binario.bin";
    private static final String PATH_DECODED  = "../Woofman/resources/videoDecod.mp4";

    public static void main( String[] args )  {

   	    Encoder encoder = new Encoder( PATH_ORIGINAL, PATH_ENCODED );
        encoder.encode();
        encoder.checkCompression();
        // encoder.checkTables();
        // encoder.checkTree();

        Decoder decoder = new Decoder( PATH_ENCODED, PATH_DECODED );
        decoder.decode();
        decoder.checkDecompression();
        // decoder.checkTables();
        // decoder.checkTree();
    }
}