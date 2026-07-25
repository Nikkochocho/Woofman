package compression.filters.bmp;

import java.io.IOException;
import java.util.Arrays;

import compression.CompressionAlgorithm;
import compression.huffman.HuffmanCoder;


public class BmpPaethHuffmanCoder implements CompressionAlgorithm  {

    private final PaethFilter   filter  = new PaethFilter();
    private final HuffmanCoder  huffman = new HuffmanCoder();
    private final BmpRle8Codec  rle8    = new BmpRle8Codec();

    @Override
    public byte[] compress( byte[] data ) throws IOException  {

        BmpHeader header = BmpHeader.parse( data );

        byte[] virtual; 

        if ( header.compression == 1 )  {
            byte[] rawGrid = rle8.decode( data, header );
            virtual = new byte[ header.dataOffset + rawGrid.length ];
            System.arraycopy( data, 0, virtual, 0, header.dataOffset );
            System.arraycopy( rawGrid, 0, virtual, header.dataOffset, rawGrid.length );
        }
        else  {
            virtual = data;
        }

        byte[] filtered = filter.apply( virtual, header );

        return huffman.compress( filtered );
    }

    @Override
    public byte[] decompress( byte[] compressedData ) throws IOException  {

        byte[]    filtered = huffman.decompress( compressedData );
        BmpHeader header   = BmpHeader.parse( filtered );
        byte[]    virtual  = filter.reverse( filtered, header );

        if ( header.compression == 1 )  {
            byte[] rawGrid = Arrays.copyOfRange( virtual, header.dataOffset, header.dataOffset + header.dataLength );
            byte[] rleData = rle8.encode( rawGrid, header );

            byte[] result = new byte[ header.dataOffset + rleData.length ];
            System.arraycopy( virtual, 0, result, 0, header.dataOffset );
            System.arraycopy( rleData, 0, result, header.dataOffset, rleData.length );
            return result;
        }

        return virtual;
    }
}