package compression.entropy.huffman;

import java.io.IOException;
import java.util.*;
import util.BitReader;


public class CanonicalHuffman  {

    private CanonicalHuffman()  {}

    private static void walk( BNode node, int depth, Map<Integer, Integer> lengths )  {

        if ( node.isLeaf() )  {
            lengths.put( node.getCharacter(), depth );
            return;
        }

        if ( node.getLeft()  != null )  walk( node.getLeft(),  depth + 1, lengths );
        if ( node.getRight() != null )  walk( node.getRight(), depth + 1, lengths );
    }

    public static Map<Integer, Integer> computeLengths( BNode root )  {

        Map<Integer, Integer> lengths = new LinkedHashMap<>();
        if ( root != null )  walk( root, 0, lengths );
        return lengths;
    }

    public static Map<Integer, String> buildCanonicalCodes( Map<Integer, Integer> lengths )  {

        List<Integer> symbols = new ArrayList<>( lengths.keySet() );
        symbols.sort( Comparator.comparingInt( (Integer s) -> lengths.get(s) ).thenComparingInt( s -> s ) );

        Map<Integer, String> codes = new LinkedHashMap<>();

        int code       = 0;
        int prevLength = 0;

        for ( int symbol : symbols )  {

            int length = lengths.get( symbol );
            code <<= ( length - prevLength );

            StringBuilder bits = new StringBuilder( Integer.toBinaryString( code ) );
            while ( bits.length() < length )  {
                bits.insert( 0, '0' );
            }

            codes.put( symbol, bits.toString() );

            code++;
            prevLength = length;
        }

        return codes;
    }

    public static Map<String, Integer> invert( Map<Integer, String> codes )  {

        Map<String, Integer> inverted = new HashMap<>();
        for ( Map.Entry<Integer, String> entry : codes.entrySet() )  {
            inverted.put( entry.getValue(), entry.getKey() );
        }
        return inverted;
    }

    public static int decodeSymbol( BitReader bitReader, Map<String, Integer> codeToSymbol ) throws IOException  {

        StringBuilder current = new StringBuilder();

        while ( true )  {
            current.append( bitReader.readBit() == 1 ? '1' : '0' );
            Integer symbol = codeToSymbol.get( current.toString() );
            if ( symbol != null )  {
                return symbol;
            }
        }
    }
}