package compression.blocksort;

import compression.CompressionAlgorithm;
import compression.entropy.model.AdaptiveFrequencyModel;
import compression.entropy.model.SymbolModel;
import compression.entropy.range.RangeDecoder;
import compression.entropy.range.RangeEncoder;
import java.io.*;


public class BWTCoder implements CompressionAlgorithm  {

    // Same as bzip2
    private static final int BLOCK_SIZE    = 900_000;
    private static final int ALPHABET_SIZE = 256;

    @Override
    public byte[] compress( byte[] data ) throws IOException  {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream( baos );

        int blockCount = data.length == 0 ? 0 : (int) Math.ceil( data.length / (double) BLOCK_SIZE );
        dos.writeInt( blockCount );

        if ( blockCount == 0 )  {
            dos.flush();
            return baos.toByteArray();
        }

        RangeEncoder encoder = new RangeEncoder( dos );                         // using range coder as entropy algorithm

        for ( int offset = 0; offset < data.length; offset += BLOCK_SIZE )  {

            int    length = Math.min( BLOCK_SIZE, data.length - offset );
            byte[] block  = new byte[length];
            System.arraycopy( data, offset, block, 0, length );

            BWT.Result bwtResult = BWT.transform( block );
            byte[]     mtf       = MTF.encode( bwtResult.transformed );

            dos.writeInt( length );
            dos.writeInt( bwtResult.index );

            SymbolModel model = new AdaptiveFrequencyModel( ALPHABET_SIZE );

            for ( byte b : mtf )  {

                int symbol = b & 0xFF;
                encoder.encode( model.cumStart( symbol ), model.freqOf( symbol ), model.totalFreq() );
                model.increment( symbol );
            }
        }

        encoder.finish();
        dos.flush();

        return baos.toByteArray();
    }

    @Override
    public byte[] decompress( byte[] compressedData ) throws IOException  {

        DataInputStream dis = new DataInputStream( new ByteArrayInputStream( compressedData ) );

        int blockCount = dis.readInt();

        if ( blockCount == 0 )  return new byte[0];

        RangeDecoder decoder = new RangeDecoder( dis );

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        for ( int b = 0; b < blockCount; b++ )  {

            int length = dis.readInt();
            int index  = dis.readInt();

            SymbolModel model = new AdaptiveFrequencyModel( ALPHABET_SIZE );
            byte[]      mtf   = new byte[length];

            for ( int i = 0; i < length; i++ )  {

                int value = decoder.getFreqValue( model.totalFreq() );
                int idx   = model.findIndex( value );
                int symbol = model.symbolAt( idx );

                decoder.decode( model.cumStartAt( idx ), model.freqAt( idx ), model.totalFreq() );
                model.increment( symbol );

                mtf[i] = (byte) symbol;
            }

            byte[] transformed = MTF.decode( mtf );
            byte[] block       = BWT.inverseTransform( transformed, index );

            output.write( block );
        }

        return output.toByteArray();
    }
}