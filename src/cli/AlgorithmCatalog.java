package cli;

import compression.CompressionType;
import java.util.ArrayList;
import java.util.List;


public final class AlgorithmCatalog  {

    private AlgorithmCatalog()  {}

    private static final List<CompressionType> GENERIC = List.of(
        CompressionType.HUFFMAN,
        CompressionType.LZ77_HUFFMAN,
        CompressionType.LZW,
        CompressionType.LZW_HUFFMAN,
        CompressionType.RANGE,
        CompressionType.LZ77_RANGE,
        CompressionType.LZW_RANGE
    );

    private static List<CompressionType> withExtras( CompressionType... extras )  {

        List<CompressionType> combined = new ArrayList<>( GENERIC );
        combined.addAll( List.of( extras ) );
        return combined;
    }

    private static boolean matchesFriendlyName( CompressionType type, String input )  {
        
        return switch ( type )  {
            case LZ77_HUFFMAN   -> input.equals( "deflate" ) || input.equals( "lz77+huffman" );
            case LZ77_ONLY      -> input.equals( "lz77" );
            case DELTA_HUFFMAN  -> input.equals( "delta" );
            case PAETH_HUFFMAN  -> input.equals( "paeth" );
            case LZW            -> input.equals( "lzw" );
            case LZW_HUFFMAN    -> input.equals( "lzw+huffman" );
            case RANGE          -> input.equals( "range" );
            case LZ77_RANGE     -> input.equals( "lzma" ) || input.equals( "lz77+range" );
            case LZW_RANGE      -> input.equals( "lzw+range" );
            default             -> false;
        };
    }

    public static List<CompressionType> suggestOptions( FileKind kind )  {

        return switch ( kind )  {
            case WAV     -> withExtras( CompressionType.RLE, CompressionType.LZ77_ONLY, CompressionType.DELTA_HUFFMAN );
            case BMP     -> withExtras( CompressionType.RLE, CompressionType.LZ77_ONLY, CompressionType.PAETH_HUFFMAN );
            case TEXT    -> withExtras();
            case UNKNOWN -> withExtras( CompressionType.RLE );
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
            case RANGE         -> "Range Coding";
            case LZ77_RANGE    -> "LZMA";
            case LZW_RANGE     -> "LZW+Range";
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