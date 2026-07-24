package compression.lz77;

import compression.CompressionAlgorithm;
import compression.huffman.BNode;
import compression.huffman.BTree;
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

    private int decodeSymbol( BitReader bitReader, BNode root, boolean single ) throws IOException  {

        if ( single )  {
            return root.getLeft().getCharacter();
        }

        BNode current = root;

        while ( !current.isLeaf() )  {
            int bit = bitReader.readBit();
            current = ( bit == 0 ) ? current.getLeft() : current.getRight();
        }

        return current.getCharacter();
    }

    private void writeTable( DataOutputStream dos, Map<Integer, Integer> freq ) throws IOException  {

        dos.writeShort( freq.size() );
        for ( Map.Entry<Integer, Integer> entry : freq.entrySet() )  {
            dos.writeShort( entry.getKey() );
            dos.writeInt( entry.getValue() );
        }
    }

    private Map<Integer, Integer> readTable( DataInputStream dis ) throws IOException  {

        Map<Integer, Integer> table = new LinkedHashMap<>();
        short size = dis.readShort();

        for ( int i = 0; i < size; i++ )  {
            int key   = dis.readShort();
            int value = dis.readInt();
            table.put( key, value );
        }

        return table;
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
        Map<Integer, String> symbolCodes = symbolTree.getConversionTable();

        Map<Integer, String> distanceCodes = Collections.emptyMap();  

        if ( hasMatches )  {                            
            BTree distanceTree = new BTree();
            distanceTree.setHeaderTable( distanceFreq );
            distanceTree.buildTree();
            distanceCodes = distanceTree.getConversionTable();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream( baos );

        writeTable( dos, symbolFreq );
        writeTable( dos, distanceFreq );
        dos.writeInt( tokens.size() );

        BitWriter bitWriter = new BitWriter( dos );

        for ( LZ77Token token : tokens )  {
            switch ( token )  {
                case Literal literal ->  {
                    int symbol = literal.value() & 0xFF;
                    bitWriter.writeBits( symbolCodes.get( symbol ) );
                }
                case Match match ->  {
                    int symbol = lengthToSymbol( match.length() );
                    bitWriter.writeBits( symbolCodes.get( symbol ) );
                    bitWriter.writeBits( distanceCodes.get( match.distance() ) );
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

        Map<Integer, Integer> symbolFreq   = readTable( dis );
        Map<Integer, Integer> distanceFreq = readTable( dis );

        BTree symbolTree = new BTree();
        symbolTree.setHeaderTable( symbolFreq );
        symbolTree.buildTree();

        BNode   symbolRoot     = symbolTree.getRoot();
        BNode   distanceRoot   = null;                          
        boolean distanceSingle = false;                     

        if ( !distanceFreq.isEmpty() )  {                   
            BTree distanceTree = new BTree();
            distanceTree.setHeaderTable( distanceFreq );
            distanceTree.buildTree();

            distanceRoot   = distanceTree.getRoot();
            distanceSingle = distanceRoot.getLeft() != null && distanceRoot.getRight() == null;
        }

        int totalTokens = dis.readInt();

        BitReader bitReader = new BitReader( dis, dis.available() );

        List<LZ77Token> tokens = new ArrayList<>( totalTokens );

        boolean symbolSingle = symbolRoot != null && symbolRoot.getLeft() != null && symbolRoot.getRight() == null;

        for ( int t = 0; t < totalTokens; t++ )  {

            int symbol = decodeSymbol( bitReader, symbolRoot, symbolSingle );

            if ( symbol < LENGTH_SYMBOL_BASE )  {
                tokens.add( new Literal( (byte) symbol ) );
            }
            else  {
                int length   = symbolToLength( symbol );
                int distance = decodeSymbol( bitReader, distanceRoot, distanceSingle ); 
                tokens.add( new Match( distance, length ) );
            }
        }

        return lz77.detokenize( tokens );
    }
}