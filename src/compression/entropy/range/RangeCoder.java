package compression.entropy.range;

import compression.CompressionAlgorithm;
import compression.entropy.FrequencyTableCodec;
import compression.entropy.model.AdaptiveFrequencyModel;
import compression.entropy.model.ArrayFrequencyModel;
import compression.entropy.model.ModelSelector;
import compression.entropy.model.SymbolModel;
import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;


public class RangeCoder implements CompressionAlgorithm  {

    private static final int MAX_TOTAL     = 1 << 15;
    private static final int ALPHABET_SIZE = 256;

    private int[] countRaw( byte[] data )  {
        int[] raw = new int[256];
        for ( byte b : data )  raw[ b & 0xFF ]++;
        return raw;
    }

    private int[] scale( int[] raw, long sum )  {

        if ( sum <= MAX_TOTAL )  return raw;

        int[] scaled    = new int[256];
        long  scaledSum = 0;

        for ( int i = 0; i < 256; i++ )  {
            if ( raw[i] > 0 )  {
                scaled[i]  = (int) Math.max( 1, ( raw[i] * (long) MAX_TOTAL ) / sum );
                scaledSum += scaled[i];
            }
        }

        while ( scaledSum > MAX_TOTAL )  {
            int maxIdx = -1;
            for ( int i = 0; i < 256; i++ )  {
                if ( scaled[i] > 1 && ( maxIdx == -1 || scaled[i] > scaled[maxIdx] ) )  maxIdx = i;
            }
            scaled[maxIdx]--;
            scaledSum--;
        }

        return scaled;
    }

    private void writeFrequencyTable( DataOutputStream dos, int[] freq ) throws IOException  {
        Map<Integer, Integer> table = new LinkedHashMap<>();
        for ( int symbol = 0; symbol < 256; symbol++ )  {
            if ( freq[symbol] > 0 )  table.put( symbol, freq[symbol] );
        }
        FrequencyTableCodec.writeSparseTable( dos, table );
    }

    private int[] readFrequencyTable( DataInputStream dis ) throws IOException  {
        int[] freq = new int[256];
        for ( Map.Entry<Integer, Integer> entry : FrequencyTableCodec.readSparseTable( dis ).entrySet() )  {
            freq[ entry.getKey() ] = entry.getValue();
        }
        return freq;
    }

    private Map<Integer, Integer> toMap( int[] freq )  {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        for ( int symbol = 0; symbol < 256; symbol++ )  {
            if ( freq[symbol] > 0 )  map.put( symbol, freq[symbol] );
        }
        return map;
    }

    private int measureHeaderBytes( int[] freq ) throws IOException  {
        ByteArrayOutputStream probe = new ByteArrayOutputStream();
        writeFrequencyTable( new DataOutputStream( probe ), freq );
        return probe.size();
    }

    @Override
    public byte[] compress( byte[] data ) throws IOException  {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream( baos );

        dos.writeInt( data.length );

        if ( data.length == 0 )  {
            dos.writeByte( 0 );
            writeFrequencyTable( dos, new int[256] );
            dos.flush();
            return baos.toByteArray();
        }

        int[] raw        = countRaw( data );
        int[] scaledFreq = scale( raw, data.length );

        int staticHeaderBytes   = measureHeaderBytes( scaledFreq );
        int adaptiveHeaderBytes = 0;   

        boolean useAdaptive = ModelSelector.shouldUseAdaptive( toMap( raw ), data.length, staticHeaderBytes, adaptiveHeaderBytes );

        dos.writeByte( useAdaptive ? 1 : 0 );

        SymbolModel model;
        if ( useAdaptive )  {
            model = new AdaptiveFrequencyModel( ALPHABET_SIZE );
        }
        else  {
            writeFrequencyTable( dos, scaledFreq );
            model = new ArrayFrequencyModel( scaledFreq );
        }

        RangeEncoder encoder = new RangeEncoder( dos );

        for ( byte b : data )  {
            int symbol = b & 0xFF;
            encoder.encode( model.cumStart( symbol ), model.freqOf( symbol ), model.totalFreq() );
            model.increment( symbol );
        }

        encoder.finish();
        dos.flush();

        return baos.toByteArray();
    }

    @Override
    public byte[] decompress( byte[] compressedData ) throws IOException  {

        DataInputStream dis = new DataInputStream( new ByteArrayInputStream( compressedData ) );

        int     originalLength = dis.readInt();
        boolean useAdaptive    = dis.readByte() == 1;

        if ( originalLength == 0 )  {
            readFrequencyTable( dis );
            return new byte[0];
        }

        SymbolModel model;
        if ( useAdaptive )  {
            model = new AdaptiveFrequencyModel( ALPHABET_SIZE );
        }
        else  {
            model = new ArrayFrequencyModel( readFrequencyTable( dis ) );
        }

        RangeDecoder decoder = new RangeDecoder( dis );

        ByteArrayOutputStream output = new ByteArrayOutputStream( originalLength );

        for ( int n = 0; n < originalLength; n++ )  {

            int value  = decoder.getFreqValue( model.totalFreq() );
            int index  = model.findIndex( value );
            int symbol = model.symbolAt( index );

            decoder.decode( model.cumStartAt( index ), model.freqAt( index ), model.totalFreq() );
            model.increment( symbol );

            output.write( symbol );
        }

        return output.toByteArray();
    }
}