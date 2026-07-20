package compression.huffman;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import compression.CompressionAlgorithm;
import util.BitWriter;
import util.BitReader;


public class HuffmanCoder implements CompressionAlgorithm  {

    @Override
    public byte[] compress( byte[] data ) throws IOException  {

        Map<Byte, Integer> headerTable = new HashMap<>();
        BTree              tree        = new BTree();

        for ( byte b : data )  {
            headerTable.merge( b, 1, Integer :: sum );
        }

        tree.setHeaderTable( headerTable );
        tree.buildTree();

        Map<Byte, String> conversionTable = tree.getConversionTable();

        long totalBits = 0;
        for ( byte b : data )  {
            totalBits += conversionTable.get( b ).length();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream( baos );

        dos.writeShort( headerTable.size() );
        for ( Map.Entry<Byte, Integer> entry : headerTable.entrySet() )  {
            dos.writeByte( entry.getKey() );
            dos.writeInt( entry.getValue() );
        }
        dos.writeInt( (int) totalBits );

        BitWriter bitWriter = new BitWriter( dos );
        for ( byte b : data )  {
            bitWriter.writeBits( conversionTable.get( b ) );
        }
        bitWriter.flush();

        dos.flush();
        return baos.toByteArray();
    }

    @Override
    public byte[] decompress( byte[] compressedData ) throws IOException  {

        DataInputStream dis = new DataInputStream( new ByteArrayInputStream( compressedData ) );

        Map<Byte, Integer> headerTable = new LinkedHashMap<>();
        short tableSize = dis.readShort();

        for ( int i = 0; i < tableSize; i++ )  {
            headerTable.put( dis.readByte(), dis.readInt() );
        }

        BTree tree = new BTree();
        tree.setHeaderTable( headerTable );
        tree.buildTree();
        BNode root = tree.getRoot();

        int totalBits = dis.readInt();

        long bytesRemaining = compressedData.length - ( compressedData.length - dis.available() );
        BitReader bitReader = new BitReader( dis, dis.available() );

        ByteArrayOutputStream baos    = new ByteArrayOutputStream();
        BNode                 current = root;

        boolean singleSymbol = root != null && root.getLeft() != null && root.getRight() == null;

        for ( int bitsRead = 0; bitsRead < totalBits; bitsRead++ )  {
            int bit = bitReader.readBit();

            if ( singleSymbol )  {
                baos.write( root.getLeft().getCharacter() );
                continue;
            }

            current = ( bit == 0 ) ? current.getLeft() : current.getRight();

            if ( current.isLeaf() )  {
                baos.write( current.getCharacter() );
                current = root;
            }
        }

        return baos.toByteArray();
    }
}