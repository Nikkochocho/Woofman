package compression.blocksort;

import compression.CompressionAlgorithm;
import compression.entropy.model.AdaptiveFrequencyModel;
import compression.entropy.model.SymbolModel;
import compression.entropy.range.RangeDecoder;
import compression.entropy.range.RangeEncoder;
import java.io.*;


public class BWTCoder implements CompressionAlgorithm  {

    private static final int BLOCK_SIZE = 900_000;          // same as BZIP2

    private final boolean useRLE;

    private static int[] toSymbols( byte[] mtf )  {

        int[] symbols = new int[ mtf.length ];
        for ( int i = 0; i < mtf.length; i++ )  symbols[i] = mtf[i] & 0xFF;
        return symbols;
    }

    private static byte[] toBytes( int[] symbols )  {

        byte[] bytes = new byte[ symbols.length ];
        for ( int i = 0; i < symbols.length; i++ )  bytes[i] = (byte) symbols[i];
        return bytes;
    }

    private int alphabetSize()  {
        return useRLE ? RLE0.ALPHABET_SIZE : 256;
    }

    public BWTCoder()  {
        this( false );
    }

    public BWTCoder( boolean useRLE )  {
        this.useRLE = useRLE;
    }

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

        // Um único RangeEncoder para o arquivo inteiro: o modelo adaptativo é
        // reiniciado a cada bloco (as estatísticas de um bloco não valem para o
        // próximo), mas o estado low/range do range coder é agnóstico a isso e
        // pode fluir continuamente -- evita reabrir o stream (4 bytes) por bloco.
        RangeEncoder encoder = new RangeEncoder( dos );

        for ( int offset = 0; offset < data.length; offset += BLOCK_SIZE )  {

            int    length = Math.min( BLOCK_SIZE, data.length - offset );
            byte[] block  = new byte[length];
            System.arraycopy( data, offset, block, 0, length );

            BWT.Result bwtResult = BWT.transform( block );
            byte[]     mtf       = MTF.encode( bwtResult.transformed );

            int[] symbols = useRLE ? RLE0.encode( mtf ) : toSymbols( mtf );

            dos.writeInt( length );
            dos.writeInt( bwtResult.index );
            dos.writeInt( symbols.length );

            SymbolModel model = new AdaptiveFrequencyModel( alphabetSize() );

            for ( int symbol : symbols )  {
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

            int length      = dis.readInt();
            int index       = dis.readInt();
            int symbolCount = dis.readInt();

            SymbolModel model   = new AdaptiveFrequencyModel( alphabetSize() );
            int[]       symbols = new int[symbolCount];

            for ( int i = 0; i < symbolCount; i++ )  {

                int value  = decoder.getFreqValue( model.totalFreq() );
                int idx    = model.findIndex( value );
                int symbol = model.symbolAt( idx );

                decoder.decode( model.cumStartAt( idx ), model.freqAt( idx ), model.totalFreq() );
                model.increment( symbol );

                symbols[i] = symbol;
            }

            byte[] mtf         = useRLE ? RLE0.decode( symbols ) : toBytes( symbols );
            byte[] transformed = MTF.decode( mtf );
            byte[] block       = BWT.inverseTransform( transformed, index );

            output.write( block );
        }

        return output.toByteArray();
    }
}