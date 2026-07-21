package cli;

import compression.CompressionType;

public class Main  {

    private static CompressionType parseAlgorithm( String name )  {
        
        return switch ( name.toLowerCase() )  {
            case "huffman" -> CompressionType.HUFFMAN;
            case "lz77"    -> CompressionType.LZ77_HUFFMAN;
            case "rle"     -> CompressionType.RLE;
            default ->  {
                System.out.println( "Unknown algorithm: " + name + ", using huffman as default." );
                yield CompressionType.HUFFMAN;
            }
        };
    }

    public static void main( String[] args )  {

        if ( args.length < 2 )  {
            Helper.help();
            return;
        }

        String command = args[0];
        String path    = args[1];

        switch ( command )  {

            case "encode" ->  {
                CompressionType type = CompressionType.HUFFMAN; // default

                for ( int i = 2; i < args.length; i++ )  {
                    if ( args[i].startsWith( "--algo=" ) )  {
                        String algoName = args[i].substring( "--algo=".length() );
                        type = parseAlgorithm( algoName );
                    }
                }

                Helper.encodeProcessing( path, type );
            }

            case "decode" -> Helper.decodeProcessing( path );

            default -> Helper.help();
        }
    }
}