import java.io.File;

public class Main  {

    public static void main( String[] args )  {

        if ( args.length < 3 )  {
            correctUsage();
            return;
        }

        String command = args[0].toLowerCase();

        try  {
            switch ( command )  {
                case "encode":
                    encodeProcessing( args[1], args[2] );
                    break;

                case "decode":
                    decodeProcessing( args[1], args[2] );
                    break;

                default:
                    System.out.println( "Invalid command: " + command );
                    correctUsage();
            }
        } catch ( Exception e )  {
            System.out.println( "Error during execution: " + e.getMessage() );
        }
    }

    private static void encodeProcessing( String pathOriginal, String pathEncoded )  {

        File originalFile = new File( pathOriginal );
        if ( !originalFile.exists() || !originalFile.isFile() )  {
            System.out.println( "File not found: " + pathOriginal );
            return;
        }

        System.out.println( "Encoding..." );
        Encoder encoder = new Encoder( pathOriginal, pathEncoded );
        encoder.encode();
        encoder.checkCompression();
        System.out.println( "Compression complete: " + pathEncoded );
    }

    private static void decodeProcessing( String pathEncoded, String pathDecoded )  {

        File encodedFile = new File( pathEncoded );
        if ( !encodedFile.exists() || !encodedFile.isFile() )  {
            System.out.println( "File not found: " + pathEncoded );
            return;
        }

        System.out.println( "Decoding..." );
        Decoder decoder = new Decoder( pathEncoded, pathDecoded );
        decoder.decode();
        decoder.checkDecompression();
        System.out.println( "Decompression complete: " + pathDecoded );
    }

    private static void correctUsage() {
        System.out.println( "\n📘 Correct usage:" );
        System.out.println( "  java Main encode <original_file> <encoded_file>" );
        System.out.println( "  java Main decode <encoded_file> <decoded_file>" );
    }
}