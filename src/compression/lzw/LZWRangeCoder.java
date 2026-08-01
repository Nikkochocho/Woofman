package compression.lzw;

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


public class LZWRangeCoder implements CompressionAlgorithm  {

    private final LZWTokenizer tokenizer = new LZWTokenizer();

    private int measureBytes( HeaderWriter writer ) throws IOException  {

        ByteArrayOutputStream probe = new ByteArrayOutputStream();
        writer.write( new DataOutputStream( probe ) );
        return probe.size();
    }

    private interface HeaderWriter  {
        void write( DataOutputStream dos ) throws IOException;
    }

    @Override
    public byte[] compress( byte[] data ) throws IOException  {

        List<Integer> codes = tokenizer.encode( data );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream( baos );

        dos.writeInt( codes.size() );

        if ( codes.isEmpty() )  {
            dos.writeByte( 0 );                                  
            FrequencyTableCodec.writeSparseTable( dos, Collections.emptyMap() );
            dos.flush();
            return baos.toByteArray();
        }

        Map<Integer, Integer> codeFreq = new HashMap<>();
        for ( int code : codes )  {
            codeFreq.merge( code, 1, Integer::sum );
        }

        Map<Integer, Integer> scaledFreq = FrequencyModel.scale( codeFreq );

        int staticHeaderBytes   = measureBytes( d -> FrequencyTableCodec.writeSparseTable( d, scaledFreq ) );
        int adaptiveHeaderBytes = measureBytes( d -> FrequencyTableCodec.writeSparseAlphabet( d, codeFreq.keySet() ) );

        boolean useAdaptive = ModelSelector.shouldUseAdaptive( codeFreq, codes.size(), staticHeaderBytes, adaptiveHeaderBytes );

        dos.writeByte( useAdaptive ? 1 : 0 );

        SymbolModel model;
        if ( useAdaptive )  {
            FrequencyTableCodec.writeSparseAlphabet( dos, codeFreq.keySet() );
            model = new SparseAdaptiveFrequencyModel( codeFreq.keySet() );
        }
        else  {
            FrequencyTableCodec.writeSparseTable( dos, scaledFreq );
            model = new FrequencyModel( scaledFreq );
        }

        RangeEncoder encoder = new RangeEncoder( dos );

        for ( int code : codes )  {
            encoder.encode( model.cumStart( code ), model.freqOf( code ), model.totalFreq() );
            model.increment( code );
        }

        encoder.finish();
        dos.flush();

        return baos.toByteArray();
    }

    @Override
    public byte[] decompress( byte[] compressedData ) throws IOException  {

        DataInputStream dis = new DataInputStream( new ByteArrayInputStream( compressedData ) );

        int     codeCount   = dis.readInt();
        boolean useAdaptive = dis.readByte() == 1;

        if ( codeCount == 0 )  {
            FrequencyTableCodec.readSparseTable( dis );           
            return tokenizer.decode( Collections.emptyList() );
        }

        SymbolModel model;
        if ( useAdaptive )  {
            List<Integer> alphabet = FrequencyTableCodec.readSparseAlphabet( dis );
            model = new SparseAdaptiveFrequencyModel( alphabet );
        }
        else  {
            Map<Integer, Integer> freq = FrequencyTableCodec.readSparseTable( dis );
            model = new FrequencyModel( freq );
        }

        RangeDecoder decoder = new RangeDecoder( dis );

        List<Integer> codes = new ArrayList<>( codeCount );

        for ( int i = 0; i < codeCount; i++ )  {

            int value = decoder.getFreqValue( model.totalFreq() );
            int idx   = model.findIndex( value );
            int code  = model.symbolAt( idx );

            decoder.decode( model.cumStartAt( idx ), model.freqAt( idx ), model.totalFreq() );
            model.increment( code );

            codes.add( code );
        }

        return tokenizer.decode( codes );
    }
}