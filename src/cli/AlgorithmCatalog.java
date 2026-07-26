package cli;

import compression.CompressionType;
import java.util.List;


public final class AlgorithmCatalog  {

    private AlgorithmCatalog()  {}

    private static boolean matchesFriendlyName( CompressionType type, String input )  {
        
        return switch ( type )  {
            case LZ77_HUFFMAN  -> input.equals( "deflate" ) || input.equals( "lz77+huffman" );
            case LZ77_ONLY     -> input.equals( "lz77" );
            case DELTA_HUFFMAN -> input.equals( "delta" );
            case PAETH_HUFFMAN -> input.equals( "paeth" );
            case LZW           -> input.equals( "lzw" );
            case LZW_HUFFMAN   -> input.equals( "lzw+huffman" );
            default            -> false;
        };
    }

    public static List<CompressionType> suggestOptions( FileKind kind )  {

        return switch ( kind )  {
            case WAV     -> List.of( CompressionType.HUFFMAN, CompressionType.LZ77_HUFFMAN, CompressionType.LZW, CompressionType.LZW_HUFFMAN, CompressionType.RLE, CompressionType.LZ77_ONLY, CompressionType.DELTA_HUFFMAN );
            case BMP     -> List.of( CompressionType.HUFFMAN, CompressionType.LZ77_HUFFMAN, CompressionType.LZW, CompressionType.LZW_HUFFMAN, CompressionType.RLE, CompressionType.LZ77_ONLY, CompressionType.PAETH_HUFFMAN );
            case TEXT    -> List.of( CompressionType.HUFFMAN, CompressionType.LZ77_HUFFMAN, CompressionType.LZW, CompressionType.LZW_HUFFMAN );
            case UNKNOWN -> List.of( CompressionType.HUFFMAN, CompressionType.LZ77_HUFFMAN, CompressionType.LZW, CompressionType.LZW_HUFFMAN, CompressionType.RLE );
        };
    }

    public static String friendlyName( CompressionType type )  {

        return switch ( type )  {
            case HUFFMAN       -> "Huffman";
            case LZ77_HUFFMAN  -> "DEFLATE";
            case RLE           -> "RLE";
            case LZ77_ONLY     -> "LZ77";
            case DELTA_HUFFMAN -> "Delta";
            case PAETH_HUFFMAN -> "Paeth";
            case LZW           -> "LZW";
            case LZW_HUFFMAN   -> "LZW + Huffman";
        };
    }

    public static String formatOptions( List<CompressionType> options )  {

        StringBuilder sb = new StringBuilder( "[" );
        for ( int i = 0; i < options.size(); i++ )  {
            sb.append( friendlyName( options.get(i) ) );
            if ( i < options.size() - 1 )  sb.append( ", " );
        }
        return sb.append( "]" ).toString();
    }

    public static CompressionType parseChoice( String input, List<CompressionType> options )  {

        if ( input.isBlank() )  return CompressionType.HUFFMAN;

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
}