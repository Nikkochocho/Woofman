package compression.lz77;

import compression.CompressionAlgorithm;
import compression.entropy.FrequencyTableCodec;
import compression.entropy.huffman.BNode;
import compression.entropy.huffman.BTree;
import compression.entropy.huffman.CanonicalHuffman;
import java.io.*;
import java.util.*;
import util.BitReader;
import util.BitWriter;


public class LZ77HuffmanCoder implements CompressionAlgorithm  {

    private static final int LENGTH_SYMBOL_BASE = 256;
    private static final int MIN_MATCH          = 3;

    private final LZ77Coder lz77 = new LZ77Coder();

    private int lengthToSymbol( int length )  {
        return LENGTH_SYMBOL_BASE + ( length - MIN_MATCH );
    }

    private int symbolToLength( int symbol )  {
        return ( symbol - LENGTH_SYMBOL_BASE ) + MIN_MATCH;
    }

    @Override
    public byte[] compress( byte[] data ) throws IOException  {

        List<LZ77Token> tokens = lz77.tokenize( data );

        Map<Integer, Integer> symbolFreq   = new HashMap<>();
        Map<Integer, Integer> distanceFreq = new HashMap<>();

        for ( LZ77Token token : tokens )  {
            switch ( token )  {
                case Literal literal ->  {
                    int symbol = literal.value() & 0xFF;
                    symbolFreq.merge( symbol, 1, Integer::sum );
                }
                case Match match ->  {
                    int symbol = lengthToSymbol( match.length() );
                    symbolFreq.merge( symbol, 1, Integer::sum );
                    distanceFreq.merge( match.distance(), 1, Integer::sum );
                }
            }
        }

        boolean hasMatches = !distanceFreq.isEmpty();

        BTree symbolTree = new BTree();
        symbolTree.setHeaderTable( symbolFreq );
        symbolTree.buildTree();
        BNode symbolRoot = symbolTree.getRoot();

        boolean symbolSingle = symbolRoot != null && symbolRoot.getLeft() != null && symbolRoot.getRight() == null;

        Map<Integer, Integer> symbolLengths = symbolSingle
                ? Map.of( symbolRoot.getLeft().getCharacter(), 0 )
                : CanonicalHuffman.computeLengths( symbolRoot );

        Map<Integer, String> symbolCodes = symbolSingle
                ? Map.of()
                : CanonicalHuffman.buildCanonicalCodes( symbolLengths );

        Map<Integer, Integer> distanceLengths = Map.of();
        Map<Integer, String>  distanceCodes   = Map.of();
        boolean                distanceSingle  = false;

        if ( hasMatches )  {
            BTree distanceTree = new BTree();
            distanceTree.setHeaderTable( distanceFreq );
            distanceTree.buildTree();
            BNode distanceRoot = distanceTree.getRoot();

            distanceSingle = distanceRoot.getLeft() != null && distanceRoot.getRight() == null;

            distanceLengths = distanceSingle
                    ? Map.of( distanceRoot.getLeft().getCharacter(), 0 )
                    : CanonicalHuffman.computeLengths( distanceRoot );

            distanceCodes = distanceSingle
                    ? Map.of()
                    : CanonicalHuffman.buildCanonicalCodes( distanceLengths );
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream( baos );

        FrequencyTableCodec.writeLengthTable( dos, symbolLengths );
        FrequencyTableCodec.writeLengthTable( dos, distanceLengths );
        dos.writeInt( tokens.size() );

        BitWriter bitWriter = new BitWriter( dos );

        for ( LZ77Token token : tokens )  {
            switch ( token )  {
                case Literal literal ->  {
                    int symbol = literal.value() & 0xFF;
                    if ( !symbolSingle )  bitWriter.writeBits( symbolCodes.get( symbol ) );
                }
                case Match match ->  {
                    int symbol = lengthToSymbol( match.length() );
                    if ( !symbolSingle )    bitWriter.writeBits( symbolCodes.get( symbol ) );
                    if ( !distanceSingle )  bitWriter.writeBits( distanceCodes.get( match.distance() ) );
                }
            }
        }

        bitWriter.flush();
        dos.flush();

        return baos.toByteArray();
    }

    @Override
    public byte[] decompress( byte[] compressedData ) throws IOException  {

        DataInputStream dis = new DataInputStream( new ByteArrayInputStream( compressedData ) );

        Map<Integer, Integer> symbolLengths   = FrequencyTableCodec.readLengthTable( dis );
        Map<Integer, Integer> distanceLengths = FrequencyTableCodec.readLengthTable( dis );

        boolean symbolSingle   = symbolLengths.size() == 1;
        boolean distanceSingle = distanceLengths.size() == 1;
        boolean hasMatches     = !distanceLengths.isEmpty();

        Map<String, Integer> symbolCodeToSymbol = symbolSingle
                ? Map.of()
                : CanonicalHuffman.invert( CanonicalHuffman.buildCanonicalCodes( symbolLengths ) );

        Map<String, Integer> distanceCodeToSymbol = ( hasMatches && !distanceSingle )
                ? CanonicalHuffman.invert( CanonicalHuffman.buildCanonicalCodes( distanceLengths ) )
                : Map.of();

        int totalTokens = dis.readInt();

        BitReader bitReader = new BitReader( dis, dis.available() );

        List<LZ77Token> tokens = new ArrayList<>( totalTokens );

        int symbolSingleValue   = symbolSingle   ? symbolLengths.keySet().iterator().next()   : -1;
        int distanceSingleValue = distanceSingle ? distanceLengths.keySet().iterator().next() : -1;

        for ( int t = 0; t < totalTokens; t++ )  {

            int symbol = symbolSingle
                    ? symbolSingleValue
                    : CanonicalHuffman.decodeSymbol( bitReader, symbolCodeToSymbol );

            if ( symbol < LENGTH_SYMBOL_BASE )  {
                tokens.add( new Literal( (byte) symbol ) );
            }
            else  {
                int length   = symbolToLength( symbol );
                int distance = distanceSingle
                        ? distanceSingleValue
                        : CanonicalHuffman.decodeSymbol( bitReader, distanceCodeToSymbol );
                tokens.add( new Match( distance, length ) );
            }
        }

        return lz77.detokenize( tokens );
    }
}