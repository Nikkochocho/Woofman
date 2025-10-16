import java.io.File;


public class helper {

    private static void timeMeasure( Runnable process )  {

        long start = System.nanoTime();

        try  {
            process.run();
        } finally {
            long end = System.nanoTime();
            System.out.printf( "Finished execution in %.3f ms%n", ( end - start ) / 1_000_000.0 );
        }
    }
    
    public static void encodeProcessing( String pathOriginal, String pathEncoded )  {

        File originalFile = new File( pathOriginal );
        if ( !originalFile.exists() || !originalFile.isFile() )  {
            System.out.println( "File not found: " + pathOriginal );
            return;
        }

        System.out.println( "Encoding..." );
        Encoder encoder = new Encoder( pathOriginal, pathEncoded );
        timeMeasure( () -> encoder.encode() );
        encoder.checkCompression();
        System.out.println( "Compression complete: " + pathEncoded );
    }

    public static void decodeProcessing( String pathEncoded, String pathDecoded )  {

        File encodedFile = new File( pathEncoded );
        if ( !encodedFile.exists() || !encodedFile.isFile() )  {
            System.out.println( "File not found: " + pathEncoded );
            return;
        }

        System.out.println( "Decoding..." );
        Decoder decoder = new Decoder( pathEncoded, pathDecoded );
        timeMeasure( () -> decoder.decode() );
        decoder.checkDecompression();
        System.out.println( "Decompression complete: " + pathDecoded );
    }

    public static void help() {
        System.out.println( "\n📘 Correct usage:" );
        System.out.println( "  java Main encode <original_file> <encoded_file>" );
        System.out.println( "  java Main decode <encoded_file> <decoded_file>" );
    }
}
