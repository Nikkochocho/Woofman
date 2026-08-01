package compression.entropy.huffman;

import compression.CompressionAlgorithm;
import compression.entropy.FrequencyTableCodec;
import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import util.BitReader;
import util.BitWriter;


public class HuffmanCoder implements CompressionAlgorithm  {

    @Override
    public byte[] compress( byte[] data ) throws IOException  {

        Map<Integer, Integer> freq = new HashMap<>();
        for ( byte b : data )  {
            freq.merge( b & 0xFF, 1, Integer::sum );
        }

        BTree tree = new BTree();
        tree.setHeaderTable( freq );
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
        dos.writeInt( data.length );

        BitWriter bitWriter = new BitWriter( dos );
        if ( !single )  {
            for ( byte b : data )  {
                bitWriter.writeBits( canonicalCodes.get( b & 0xFF ) );
            }
        }
        bitWriter.flush();

        dos.flush();
        return baos.toByteArray();
    }

    @Override
    public byte[] decompress( byte[] compressedData ) throws IOException  {

        DataInputStream dis = new DataInputStream( new ByteArrayInputStream( compressedData ) );

        Map<Integer, Integer> lengths = FrequencyTableCodec.readLengthTable( dis );
        boolean                single = lengths.size() == 1;

        int totalBytes = dis.readInt();

        BitReader bitReader = new BitReader( dis, dis.available() );

        ByteArrayOutputStream output = new ByteArrayOutputStream( totalBytes );

        if ( single )  {

            int symbol = lengths.keySet().iterator().next();
            for ( int i = 0; i < totalBytes; i++ )  {
                output.write( symbol );
            }
        }
        else  {

            Map<Integer, String> canonicalCodes = CanonicalHuffman.buildCanonicalCodes( lengths );
            Map<String, Integer> codeToSymbol   = CanonicalHuffman.invert( canonicalCodes );

            for ( int i = 0; i < totalBytes; i++ )  {
                output.write( CanonicalHuffman.decodeSymbol( bitReader, codeToSymbol ) );
            }
        }

        return output.toByteArray();
    }
}