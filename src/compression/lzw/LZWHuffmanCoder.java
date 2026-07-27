package compression.lzw;

import compression.CompressionAlgorithm;
import compression.entropy.FrequencyTableCodec;
import compression.entropy.huffman.BNode;
import compression.entropy.huffman.BTree;
import compression.entropy.huffman.CanonicalHuffman;
import java.io.*;
import java.util.*;
import util.BitReader;
import util.BitWriter;


public class LZWHuffmanCoder implements CompressionAlgorithm  {

    private final LZWTokenizer tokenizer = new LZWTokenizer();

    @Override
    public byte[] compress( byte[] data ) throws IOException  {

        List<Integer> codes = tokenizer.encode( data );

        Map<Integer, Integer> codeFreq = new HashMap<>();
        for ( int code : codes )  {
            codeFreq.merge( code, 1, Integer::sum );
        }

        BTree tree = new BTree();
        tree.setHeaderTable( codeFreq );
        tree.buildTree();
        BNode root = tree.getRoot();

        boolean single = root != null && root.getLeft() != null && root.getRight() == null;

        Map<Integer, Integer> lengths;
        Map<Integer, String>  canonicalCodes;

        if ( single )  {
            lengths        = new LinkedHashMap<>();
            lengths.put( root.getLeft().getCharacter(), 0 );  
            canonicalCodes = Map.of();
        }
        else  {
            lengths        = CanonicalHuffman.computeLengths( root );
            canonicalCodes = CanonicalHuffman.buildCanonicalCodes( lengths );
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream( baos );

        FrequencyTableCodec.writeLengthTable( dos, lengths );
        dos.writeInt( codes.size() );

        BitWriter bitWriter = new BitWriter( dos );
        if ( !single )  {
            for ( int code : codes )  {
                bitWriter.writeBits( canonicalCodes.get( code ) );
            }
        }
        bitWriter.flush();
        dos.flush();

        return baos.toByteArray();
    }

    @Override
    public byte[] decompress( byte[] compressedData ) throws IOException  {

        DataInputStream dis = new DataInputStream( new ByteArrayInputStream( compressedData ) );

        Map<Integer, Integer>  lengths = FrequencyTableCodec.readLengthTable( dis );
        boolean                single  = lengths.size() == 1;

        int totalTokens = dis.readInt();

        BitReader bitReader = new BitReader( dis, dis.available() );

        List<Integer> codes = new ArrayList<>( totalTokens );

        if ( single )  {
            int symbol = lengths.keySet().iterator().next();
            for ( int t = 0; t < totalTokens; t++ )  {
                codes.add( symbol );
            }
        }
        else  {
            Map<Integer, String> canonicalCodes = CanonicalHuffman.buildCanonicalCodes( lengths );
            Map<String, Integer> codeToSymbol   = CanonicalHuffman.invert( canonicalCodes );

            for ( int t = 0; t < totalTokens; t++ )  {
                codes.add( CanonicalHuffman.decodeSymbol( bitReader, codeToSymbol ) );
            }
        }

        return tokenizer.decode( codes );
    }
}