package compression.lzw;

import compression.CompressionAlgorithm;
import compression.entropy.FrequencyTableCodec;
import compression.entropy.arithmetic.ArithmeticDecoder;
import compression.entropy.arithmetic.ArithmeticEncoder;
import compression.entropy.model.FrequencyModel;
import compression.entropy.model.ModelSelector;
import compression.entropy.model.SparseAdaptiveFrequencyModel;
import compression.entropy.model.SymbolModel;
import java.io.*;
import java.util.*;
import util.BitReader;
import util.BitWriter;


public class LZWArithmeticCoder implements CompressionAlgorithm  {

    private final LZWTokenizer tokenizer = new LZWTokenizer();

    private interface HeaderWriter  {

        void write( DataOutputStream dos ) throws IOException;
    }

    private byte[] probeBytes( HeaderWriter writer ) throws IOException  {

        ByteArrayOutputStream probe = new ByteArrayOutputStream();
        writer.write( new DataOutputStream( probe ) );
        return probe.toByteArray();
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

        byte[] staticHeader   = probeBytes( d -> FrequencyTableCodec.writeSparseTable( d, scaledFreq ) );
        byte[] adaptiveHeader = probeBytes( d -> FrequencyTableCodec.writeSparseAlphabet( d, codeFreq.keySet() ) );

        boolean useAdaptive = ModelSelector.shouldUseAdaptive( codeFreq, codes.size(), staticHeader.length, adaptiveHeader.length );

        dos.writeByte( useAdaptive ? 1 : 0 );

        SymbolModel model;
        if ( useAdaptive )  {
            dos.write( adaptiveHeader );
            model = new SparseAdaptiveFrequencyModel( codeFreq.keySet() );
        }
        else  {
            dos.write( staticHeader );
            model = new FrequencyModel( scaledFreq );
        }

        BitWriter          bitWriter = new BitWriter( dos );
        ArithmeticEncoder   encoder  = new ArithmeticEncoder( bitWriter );

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

        BitReader          bitReader = new BitReader( dis, dis.available() );
        ArithmeticDecoder   decoder  = new ArithmeticDecoder( bitReader );

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