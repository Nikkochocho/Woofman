package compression.entropy.range;

import compression.CompressionAlgorithm;
import java.io.*;


public class AdaptiveRangeCoder implements CompressionAlgorithm  {

    private static final int ALPHABET_SIZE = 256;

    @Override
    public byte[] compress( byte[] data ) throws IOException  {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream( baos );

        dos.writeInt( data.length );      

        AdaptiveFrequencyModel model   = new AdaptiveFrequencyModel( ALPHABET_SIZE );
        RangeEncoder           encoder = new RangeEncoder( dos );

        for ( byte b : data )  {

            int symbol = ( b & 0xFF ) + 1;    

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

        int originalLength = dis.readInt();

        AdaptiveFrequencyModel model   = new AdaptiveFrequencyModel( ALPHABET_SIZE );
        RangeDecoder            decoder = new RangeDecoder( dis );

        ByteArrayOutputStream output = new ByteArrayOutputStream( originalLength );

        for ( int i = 0; i < originalLength; i++ )  {

            int total  = model.totalFreq();
            int value  = decoder.getFreqValue( total );
            int symbol = model.findByCumFreq( value );

            decoder.decode( model.cumStart( symbol ), model.freqOf( symbol ), total );
            output.write( symbol - 1 );

            model.increment( symbol );
        }

        return output.toByteArray();
    }
}