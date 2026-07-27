package compression.lz77;

import compression.CompressionAlgorithm;
import compression.entropy.FrequencyTableCodec;
import compression.entropy.range.FrequencyModel;
import compression.entropy.range.RangeDecoder;
import compression.entropy.range.RangeEncoder;
import java.io.*;
import java.util.*;


public class LZ77RangeCoder implements CompressionAlgorithm  {

    private static final int LENGTH_SYMBOL_BASE = 256;
    private static final int MIN_MATCH          = 3;

    private final LZ77Coder lz77 = new LZ77Coder();

    private int lengthToSymbol( int length )  { return LENGTH_SYMBOL_BASE + ( length - MIN_MATCH ); }
    private int symbolToLength( int symbol )  { return ( symbol - LENGTH_SYMBOL_BASE ) + MIN_MATCH; }

    @Override
    public byte[] compress( byte[] data ) throws IOException  {

        List<LZ77Token> tokens = lz77.tokenize( data );

        Map<Integer, Integer> symbolFreq   = new HashMap<>();
        Map<Integer, Integer> distanceFreq = new HashMap<>();

        for ( LZ77Token token : tokens )  {
            switch ( token )  {
                case Literal literal ->  {
                    symbolFreq.merge( literal.value() & 0xFF, 1, Integer::sum );
                }
                case Match match ->  {
                    symbolFreq.merge( lengthToSymbol( match.length() ), 1, Integer::sum );
                    distanceFreq.merge( match.distance(), 1, Integer::sum );
                }
            }
        }

        symbolFreq   = FrequencyModel.scale( symbolFreq );
        distanceFreq = distanceFreq.isEmpty() ? distanceFreq : FrequencyModel.scale( distanceFreq );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream( baos );

        FrequencyTableCodec.writeSparseTable( dos, symbolFreq );
        FrequencyTableCodec.writeSparseTable( dos, distanceFreq );
        dos.writeInt( tokens.size() );

        FrequencyModel symbolModel   = new FrequencyModel( symbolFreq );
        FrequencyModel distanceModel = distanceFreq.isEmpty() ? null : new FrequencyModel( distanceFreq );

        RangeEncoder encoder = new RangeEncoder( dos );

        for ( LZ77Token token : tokens )  {
            switch ( token )  {
                case Literal literal ->  {
                    int symbol = literal.value() & 0xFF;
                    encoder.encode( symbolModel.cumStart( symbol ), symbolModel.freqOf( symbol ), symbolModel.totalFreq );
                }
                case Match match ->  {
                    int symbol = lengthToSymbol( match.length() );
                    encoder.encode( symbolModel.cumStart( symbol ), symbolModel.freqOf( symbol ), symbolModel.totalFreq );

                    int distance = match.distance();
                    encoder.encode( distanceModel.cumStart( distance ), distanceModel.freqOf( distance ), distanceModel.totalFreq );
                }
            }
        }

        encoder.finish();
        dos.flush();

        return baos.toByteArray();
    }

    @Override
    public byte[] decompress( byte[] compressedData ) throws IOException  {

        DataInputStream dis = new DataInputStream( new ByteArrayInputStream( compressedData ) );

        Map<Integer, Integer> symbolFreq   = FrequencyTableCodec.readSparseTable( dis );
        Map<Integer, Integer> distanceFreq = FrequencyTableCodec.readSparseTable( dis );
        int totalTokens = dis.readInt();

        FrequencyModel symbolModel   = new FrequencyModel( symbolFreq );
        FrequencyModel distanceModel = distanceFreq.isEmpty() ? null : new FrequencyModel( distanceFreq );

        RangeDecoder decoder = new RangeDecoder( dis );

        List<LZ77Token> tokens = new ArrayList<>( totalTokens );

        for ( int t = 0; t < totalTokens; t++ )  {

            int symbolValue = decoder.getFreqValue( symbolModel.totalFreq );
            int symbolIdx   = symbolModel.findIndex( symbolValue );
            int symbol      = symbolModel.symbolAt( symbolIdx );
            decoder.decode( symbolModel.cumStartAt( symbolIdx ), symbolModel.freqAt( symbolIdx ), symbolModel.totalFreq );

            if ( symbol < LENGTH_SYMBOL_BASE )  {
                tokens.add( new Literal( (byte) symbol ) );
            }
            else  {
                int length = symbolToLength( symbol );

                int distValue = decoder.getFreqValue( distanceModel.totalFreq );
                int distIdx   = distanceModel.findIndex( distValue );
                int distance  = distanceModel.symbolAt( distIdx );
                decoder.decode( distanceModel.cumStartAt( distIdx ), distanceModel.freqAt( distIdx ), distanceModel.totalFreq );

                tokens.add( new Match( distance, length ) );
            }
        }

        return lz77.detokenize( tokens );
    }
}