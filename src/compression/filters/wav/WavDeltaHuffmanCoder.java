package compression.filters.wav;

import java.io.IOException;

import compression.CompressionAlgorithm;
import compression.huffman.HuffmanCoder;


public class WavDeltaHuffmanCoder implements CompressionAlgorithm  {

    private final DeltaFilter  filter  = new DeltaFilter();
    private final HuffmanCoder huffman = new HuffmanCoder();

    @Override
    public byte[] compress( byte[] data ) throws IOException  {

        WavHeader header   = WavHeader.parse( data );
        byte[]    filtered = filter.apply( data, header );

        return huffman.compress( filtered );
    }

    @Override
    public byte[] decompress( byte[] compressedData ) throws IOException  {

        byte[]    filtered = huffman.decompress( compressedData );
        WavHeader header   = WavHeader.parse( filtered );
        
        return filter.reverse( filtered, header );
    }
}