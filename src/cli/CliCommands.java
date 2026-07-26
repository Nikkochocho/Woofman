package cli;

import compression.CompressionType;
import container.Decoder;
import container.Encoder;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class CliCommands  {

    private static final Scanner scanner = new Scanner( System.in );

    private static List<Path> collectFiles( Path source ) throws IOException  {

        List<Path> list = new ArrayList<>();

        if ( Files.isRegularFile( source ) )  {
            list.add( source );
            return list;
        }

        Files.walkFileTree( source, new SimpleFileVisitor<>()  {
            @Override
            public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )  {
                list.add( file );
                return FileVisitResult.CONTINUE;
            }
        });

        return list;
    }

    private static String stripExtension( String fileName )  {

        int dotIndex = fileName.lastIndexOf( '.' );
        return dotIndex > 0 ? fileName.substring( 0, dotIndex ) : fileName;
    }

    private static void timeMeasure( Runnable process )  {
        
        long start = System.nanoTime();
        try  { process.run(); }
        finally  {
            long end = System.nanoTime();
            System.out.printf( "Finished execution in %.3f ms%n", ( end - start ) / 1_000_000.0 );
        }
    }

    public static void encodeProcessing( String path )  {

        Path source = Path.of( path );
        if ( !Files.exists( source ) )  {
            System.out.println( "Not found: " + path );
            return;
        }

        List<Path> files;
        try  {
            files = collectFiles( source );
        } catch ( IOException ex )  {
            System.getLogger( CliCommands.class.getName() ).log( System.Logger.Level.ERROR, (String) null, ex );
            return;
        }

        Map<Path, CompressionType> choices = new LinkedHashMap<>();

        for ( Path file : files )  {
            FileKind kind = FileKindDetector.detect( file );
            List<CompressionType> options = AlgorithmCatalog.suggestOptions( kind );

            System.out.println( "File: " + file.getFileName() + " - Encode options " + AlgorithmCatalog.formatOptions( options ) );
            System.out.print( "> " );

            String input = scanner.nextLine();
            choices.put( file, AlgorithmCatalog.parseChoice( input, options ) );
        }

        Path outputFile = Files.isRegularFile( source )
                ? source.resolveSibling( stripExtension( source.getFileName().toString() ) + ".woof" )
                : source.resolveSibling( source.getFileName() + ".woof" );

        System.out.println( "\nENCODING..." );
        Encoder encoder = new Encoder( source, outputFile );

        timeMeasure( () -> {
            try  {
                encoder.encode( choices );
                System.out.println( "Compression complete: " + outputFile );
            } catch ( IOException ex )  {
                System.getLogger( CliCommands.class.getName() ).log( System.Logger.Level.ERROR, (String) null, ex );
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
            } catch ( Exception ex )  {
                System.getLogger(CliCommands.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        } );
    }

    public static void help()  {
        System.out.println( "\n📘 Correct usage:" );
        System.out.println( "  java -cp out cli.Main encode <source>" );
        System.out.println( "  java -cp out cli.Main decode <compressed_file>" );
        System.out.println( "\nDuring encode, you'll be prompted to choose a compression algorithm per file." );
    }
}