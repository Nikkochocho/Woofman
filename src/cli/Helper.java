package cli;

import compression.CompressionType;
import container.Decoder;
import container.Encoder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Helper  {

    private static void timeMeasure( Runnable process )  {

        long start = System.nanoTime();

        try  {
            process.run();
        } finally {
            long end = System.nanoTime();
            System.out.printf( "Finished execution in %.3f ms%n", ( end - start ) / 1_000_000.0 );
        }
    }
    
    public static void encodeProcessing( String path, CompressionType type )  {

        Path source = Path.of( path );
        if ( !Files.exists( source ) ) {
            System.out.println( "Not found: " + path );
            return;
        }

        Path outputFile = source.resolveSibling( source.getFileName() + ".woof" );

        System.out.println( "Encoding with " + type + "..." );
        Encoder encoder = new Encoder( source, outputFile, type );
        timeMeasure( () -> {
            try  {
                encoder.encode();
                System.out.println( "Compression complete: " + outputFile );
            } catch ( IOException ex )  {
                System.getLogger( Helper.class.getName()).log( System.Logger.Level.ERROR, (String) null, ex );
            }
        } );
    }

    public static void decodeProcessing( String path )  {

        Path inputFile = Path.of( path );
        if ( !Files.exists( inputFile ) || !Files.isRegularFile( inputFile ) )  {
            System.out.println( "File not found: " + path );
            return;
        }

        String fileName = inputFile.getFileName().toString();
        String dirName  = fileName.endsWith( ".woof" ) ? fileName.replace( ".woof", "" ) : fileName + "_decoded";
        Path   outputDir = inputFile.resolveSibling( dirName );

        System.out.println( "Decoding..." );
        Decoder decoder = new Decoder( inputFile, outputDir );
        timeMeasure( () -> {
            try  {
                decoder.decode();
                System.out.println( "Decompression complete: " + outputDir );
            } catch ( IOException ex )  {
                System.getLogger( Helper.class.getName()).log( System.Logger.Level.ERROR, (String) null, ex );
            }
        } );
    }

    public static void help()  {
        System.out.println( "\n📘 Correct usage:" );
        System.out.println( "  java -cp out cli.Main encode <source> [--algo=huffman|lz77]" );
        System.out.println( "  java -cp out cli.Main decode <compressed_file>" );
        System.out.println( "\nAvailable algorithms: huffman (default), lz77" );
    }
}
