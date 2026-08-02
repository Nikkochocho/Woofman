package compression.lz77;

import compression.CompressionAlgorithm;
import compression.entropy.FrequencyTableCodec;
import compression.entropy.model.FrequencyModel;
import compression.entropy.model.ModelSelector;
import compression.entropy.model.SparseAdaptiveFrequencyModel;
import compression.entropy.model.SymbolModel;
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

    private interface HeaderWriter  {
        
        void write( DataOutputStream dos ) throws IOException;
    }

    private byte[] probeBytes( HeaderWriter writer ) throws IOException  {

        ByteArrayOutputStream probe = new ByteArrayOutputStream();
        writer.write( new DataOutputStream( probe ) );
        return probe.toByteArray();
    }

    private SymbolModel selectAndWriteModel( DataOutputStream dos, Map<Integer, Integer> freq, int totalSymbols ) throws IOException  {

        if ( freq.isEmpty() )  {
            dos.writeByte( 0 );
            FrequencyTableCodec.writeSparseTable( dos, freq );
            return new FrequencyModel( freq );
        }

        Map<Integer, Integer> scaledFreq = FrequencyModel.scale( freq );

        byte[] staticHeader   = probeBytes( d -> FrequencyTableCodec.writeSparseTable( d, scaledFreq ) );
        byte[] adaptiveHeader = probeBytes( d -> FrequencyTableCodec.writeSparseAlphabet( d, freq.keySet() ) );

        boolean useAdaptive = ModelSelector.shouldUseAdaptive( freq, totalSymbols, staticHeader.length, adaptiveHeader.length );

        dos.writeByte( useAdaptive ? 1 : 0 );

        if ( useAdaptive )  {
            dos.write( adaptiveHeader );
            return new SparseAdaptiveFrequencyModel( freq.keySet() );
        }

        dos.write( staticHeader );
        return new FrequencyModel( scaledFreq );
    }

    private SymbolModel readModel( DataInputStream dis ) throws IOException  {

        boolean useAdaptive = dis.readByte() == 1;

        if ( useAdaptive )  {
            List<Integer> alphabet = FrequencyTableCodec.readSparseAlphabet( dis );
            return new SparseAdaptiveFrequencyModel( alphabet );
        }

        Map<Integer, Integer> freq = FrequencyTableCodec.readSparseTable( dis );
        return new FrequencyModel( freq );
    }

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

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream( baos );

        dos.writeInt( tokens.size() );

        if ( tokens.isEmpty() )  {
            dos.writeByte( 0 );
            FrequencyTableCodec.writeSparseTable( dos, symbolFreq );
            dos.writeByte( 0 );
            FrequencyTableCodec.writeSparseTable( dos, distanceFreq );
            dos.flush();
            return baos.toByteArray();
        }

        SymbolModel symbolModel   = selectAndWriteModel( dos, symbolFreq, tokens.size() );
        SymbolModel distanceModel;

        if ( distanceFreq.isEmpty() )  {                          
            dos.writeByte( 0 );
            FrequencyTableCodec.writeSparseTable( dos, distanceFreq );
            distanceModel = null;
        }
        else  {
            distanceModel = selectAndWriteModel( dos, distanceFreq, sumValues( distanceFreq ) );
        }

        RangeEncoder encoder = new RangeEncoder( dos );

        for ( LZ77Token token : tokens )  {
            switch ( token )  {
                case Literal literal ->  {
                    int symbol = literal.value() & 0xFF;
                    encoder.encode( symbolModel.cumStart( symbol ), symbolModel.freqOf( symbol ), symbolModel.totalFreq() );
                    symbolModel.increment( symbol );
                }
                case Match match ->  {
                    int symbol = lengthToSymbol( match.length() );
                    encoder.encode( symbolModel.cumStart( symbol ), symbolModel.freqOf( symbol ), symbolModel.totalFreq() );
                    symbolModel.increment( symbol );

                    int distance = match.distance();
                    encoder.encode( distanceModel.cumStart( distance ), distanceModel.freqOf( distance ), distanceModel.totalFreq() );
                    distanceModel.increment( distance );
                }
            }
        }

        encoder.finish();
        dos.flush();

        return baos.toByteArray();
    }

    private int sumValues( Map<Integer, Integer> freq )  {
        int sum = 0;
        for ( int f : freq.values() )  sum += f;
        return sum;
    }

    @Override
    public byte[] decompress( byte[] compressedData ) throws IOException  {

        DataInputStream dis = new DataInputStream( new ByteArrayInputStream( compressedData ) );

        int totalTokens = dis.readInt();

        if ( totalTokens == 0 )  {
            dis.readByte();
            FrequencyTableCodec.readSparseTable( dis );
            dis.readByte();
            FrequencyTableCodec.readSparseTable( dis );
            return lz77.detokenize( Collections.emptyList() );
        }

        SymbolModel symbolModel   = readModel( dis );
        SymbolModel distanceModel = readModel( dis );

        RangeDecoder decoder = new RangeDecoder( dis );

        List<LZ77Token> tokens = new ArrayList<>( totalTokens );

        for ( int t = 0; t < totalTokens; t++ )  {

            int symbolValue = decoder.getFreqValue( symbolModel.totalFreq() );
            int symbolIdx   = symbolModel.findIndex( symbolValue );
            int symbol      = symbolModel.symbolAt( symbolIdx );
            decoder.decode( symbolModel.cumStartAt( symbolIdx ), symbolModel.freqAt( symbolIdx ), symbolModel.totalFreq() );
            symbolModel.increment( symbol );

            if ( symbol < LENGTH_SYMBOL_BASE )  {
                tokens.add( new Literal( (byte) symbol ) );
            }
            else  {
                int length = symbolToLength( symbol );

                int distValue = decoder.getFreqValue( distanceModel.totalFreq() );
                int distIdx   = distanceModel.findIndex( distValue );
                int distance  = distanceModel.symbolAt( distIdx );
                decoder.decode( distanceModel.cumStartAt( distIdx ), distanceModel.freqAt( distIdx ), distanceModel.totalFreq() );
                distanceModel.increment( distance );

                tokens.add( new Match( distance, length ) );
            }
        }

        return lz77.detokenize( tokens );
    }
}