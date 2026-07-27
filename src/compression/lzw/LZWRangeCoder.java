package compression.lzw;

import compression.CompressionAlgorithm;
import compression.entropy.FrequencyTableCodec;
import compression.entropy.range.FrequencyModel;
import compression.entropy.range.RangeDecoder;
import compression.entropy.range.RangeEncoder;
import java.io.*;
import java.util.*;


public class LZWRangeCoder implements CompressionAlgorithm  {

    private final LZWTokenizer tokenizer = new LZWTokenizer();

    @Override
    public byte[] compress( byte[] data ) throws IOException  {

        List<Integer> codes = tokenizer.encode( data );

        Map<Integer, Integer> codeFreq = new HashMap<>();
        for ( int code : codes )  {
            codeFreq.merge( code, 1, Integer::sum );
        }

        codeFreq = FrequencyModel.scale( codeFreq );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream( baos );

        FrequencyTableCodec.writeSparseTable( dos, codeFreq );
        dos.writeInt( codes.size() );

        FrequencyModel  model   = new FrequencyModel( codeFreq );
        RangeEncoder    encoder = new RangeEncoder( dos );

        for ( int code : codes )  {
            encoder.encode( model.cumStart( code ), model.freqOf( code ), model.totalFreq );
        }

        encoder.finish();
        dos.flush();

        return baos.toByteArray();
    }

    @Override
    public byte[] decompress( byte[] compressedData ) throws IOException  {

        DataInputStream dis = new DataInputStream( new ByteArrayInputStream( compressedData ) );

        Map<Integer, Integer> codeFreq   = FrequencyTableCodec.readSparseTable( dis );
        int                   codeCount  = dis.readInt();

        FrequencyModel model   = new FrequencyModel( codeFreq );
        RangeDecoder    decoder = new RangeDecoder( dis );

        List<Integer> codes = new ArrayList<>( codeCount );

        for ( int i = 0; i < codeCount; i++ )  {

            int value = decoder.getFreqValue( model.totalFreq );
            int idx   = model.findIndex( value );
            int code  = model.symbolAt( idx );

            decoder.decode( model.cumStartAt( idx ), model.freqAt( idx ), model.totalFreq );
            codes.add( code );
        }

        return tokenizer.decode( codes );
    }
}