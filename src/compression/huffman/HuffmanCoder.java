package compression.huffman;

import compression.CompressionAlgorithm;
import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import util.BitReader;
import util.BitWriter;


public class HuffmanCoder implements CompressionAlgorithm  {

    @Override
    public byte[] compress( byte[] data ) throws IOException  {

        Map<Integer, Integer> headerTable = new HashMap<>();
        BTree                 tree        = new BTree();

        for ( byte b : data )  {
            int symbol = b & 0xFF;                          
            headerTable.merge( symbol, 1, Integer :: sum );
        }

        tree.setHeaderTable( headerTable );
        tree.buildTree();

        Map<Integer, String> conversionTable = tree.getConversionTable();

        long totalBits = 0;
        for ( byte b : data )  {
            totalBits += conversionTable.get( b & 0xFF ).length();  
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream( baos );

        dos.writeShort( headerTable.size() );

        for ( Map.Entry<Integer, Integer> entry : headerTable.entrySet() )  {
            dos.writeShort( entry.getKey() );                        
            dos.writeInt( entry.getValue() );
        }

        dos.writeInt( (int) totalBits );

        BitWriter bitWriter = new BitWriter( dos );
        for ( byte b : data )  {
            bitWriter.writeBits( conversionTable.get( b & 0xFF ) ); 
        }
        bitWriter.flush();

        dos.flush();
        return baos.toByteArray();
    }

    @Override
    public byte[] decompress( byte[] compressedData ) throws IOException  {

        DataInputStream dis = new DataInputStream( new ByteArrayInputStream( compressedData ) );

        Map<Integer, Integer> headerTable = new LinkedHashMap<>();
        short tableSize = dis.readShort();

        for ( int i = 0; i < tableSize; i++ )  {
            int key   = dis.readShort();      
            int value = dis.readInt();
            headerTable.put( key, value );
        }

        BTree tree = new BTree();
        tree.setHeaderTable( headerTable );
        tree.buildTree();
        BNode root = tree.getRoot();

        int totalBits = dis.readInt();

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