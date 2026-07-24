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

public class Helper  {

    private static final Scanner scanner = new Scanner( System.in );

    private static FileKind detectFileKind( Path file )  {

        String name = file.getFileName().toString().toLowerCase();

        if ( name.endsWith( ".wav" ) )  {
            return FileKind.WAV;
        }
        if ( name.endsWith( ".bmp" ) )  {
            return FileKind.BMP;
        }
        if ( name.endsWith( ".txt" ) || name.endsWith( ".md" ) || name.endsWith( ".csv" ) )  {
            return FileKind.TEXT;
        }

        return FileKind.UNKNOWN;
    }

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

    private static List<CompressionType> suggestOptions( FileKind kind )  {

        return switch ( kind )  {
            case WAV, BMP -> List.of( CompressionType.HUFFMAN, CompressionType.LZ77_HUFFMAN, CompressionType.RLE, CompressionType.LZ77_ONLY );
            case TEXT     -> List.of( CompressionType.HUFFMAN, CompressionType.LZ77_HUFFMAN );
            case UNKNOWN  -> List.of( CompressionType.HUFFMAN, CompressionType.LZ77_HUFFMAN, CompressionType.RLE );
        };
    }

    private static CompressionType parseChoice( String input, List<CompressionType> options )  {

        if ( input.isBlank() )  {
            return CompressionType.HUFFMAN;
        }

        String normalized = input.trim().toLowerCase();

        for ( CompressionType option : options )  {
            if ( option.name().equalsIgnoreCase( normalized )
                || option.name().toLowerCase().replace( "_", "" ).equals( normalized.replace( "-", "" ) )
                || matchesFriendlyName( option, normalized ) )  {
                return option;
            }
        }

        System.out.println( "Option is not available. Using Huffman as default" );
        return CompressionType.HUFFMAN;
    }

    private static boolean matchesFriendlyName( CompressionType type, String input )  {

        return switch ( type )  {
            case LZ77_HUFFMAN -> input.equals( "deflate" ) || input.equals( "lz77+huffman" );
            case LZ77_ONLY     -> input.equals( "lz77" );
            default             -> false;
        };
    }

    private static String formatOptions( List<CompressionType> options )  {

        StringBuilder sb = new StringBuilder( "[" );

        for ( int i = 0; i < options.size(); i++ )  {
            sb.append( friendlyName( options.get(i) ) );
            if ( i < options.size() - 1 )  {
                sb.append( ", " );
            }
        }

        sb.append( "]" );
        return sb.toString();
    }

    private static String friendlyName( CompressionType type )  {

        return switch ( type )  {
            case HUFFMAN       -> "Huffman";
            case LZ77_HUFFMAN  -> "DEFLATE";
            case RLE           -> "RLE";
            case LZ77_ONLY     -> "LZ77";
            // case DELTA_HUFFMAN -> "Delta";
            // case PAETH_HUFFMAN -> "Paeth";
        };
    }

    private static void timeMeasure( Runnable process )  {

        long start = System.nanoTime();

        try  {
            process.run();
        } finally {
            long end = System.nanoTime();
            System.out.printf( "Finished execution in %.3f ms%n", ( end - start ) / 1_000_000.0 );
        }
    }

    public static void encodeProcessing( String path )  {

        Path source = Path.of( path );
        if ( !Files.exists( source ) ) {
            System.out.println( "Not found: " + path );
            return;
        }

        List<Path> files;
        try  {
            files = collectFiles( source );
        } catch ( IOException ex )  {
            System.getLogger( Helper.class.getName() ).log( System.Logger.Level.ERROR, (String) null, ex );
            return;
        }

        Map<Path, CompressionType> choices = new LinkedHashMap<>();

        for ( Path file : files )  {

            FileKind kind             = detectFileKind( file );
            List<CompressionType> options = suggestOptions( kind );

            System.out.println( "File: " + file.getFileName() + " - Encode options " + formatOptions( options ) );
            System.out.print( "> " );

            String input = scanner.nextLine();
            CompressionType chosen = parseChoice( input, options );

            choices.put( file, chosen );
        }

        Path outputFile = source.resolveSibling( source.getFileName() + ".woof" );

        System.out.println( "\nENCODING..." );
        Encoder encoder = new Encoder( source, outputFile );

        timeMeasure( () -> {
            try  {
                encoder.encode( choices );
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
        System.out.println( "  java -cp out cli.Main encode <source>" );
        System.out.println( "  java -cp out cli.Main decode <compressed_file>" );
        System.out.println( "\nDuring encode, you'll be prompted to choose a compression algorithm per file." );
    }
}