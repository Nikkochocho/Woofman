package compression.entropy.arithmetic;

import java.io.IOException;
import util.BitReader;


public class ArithmeticDecoder  {

    private static final long TOP        = 0xFFFFFFFFL;
    private static final long HALF       = 0x80000000L;
    private static final long QUARTER    = 0x40000000L;
    private static final long THREE_QRTR = 0xC0000000L;
    private static final int  CODE_BITS  = 32;

    private final BitReader bitReader;

    private long low   = 0;
    private long high  = TOP;
    private long value = 0;

    public ArithmeticDecoder( BitReader bitReader ) throws IOException  {

        this.bitReader = bitReader;

        for ( int i = 0; i < CODE_BITS; i++ )  {
            value = ( ( value << 1 ) | nextBit() ) & TOP;
        }
    }

    // Após o último símbolo o fluxo de bits acaba antes dos 32 bits do registrador
    // terminarem de deslocar; preenche com zero, igual ao BOT-underflow do RangeDecoder.
    private int nextBit() throws IOException  {

        try  { return bitReader.readBit(); }
        catch ( IOException ex )  { return 0; }
    }

    public int getFreqValue( int totalFreq )  {

        long range = ( high - low + 1 );
        return (int) Math.min( totalFreq - 1, ( ( value - low + 1 ) * totalFreq - 1 ) / range );
    }

    public void decode( int cumStart, int freq, int totalFreq ) throws IOException  {

        long range = ( high - low + 1 );

        high = low + ( range * ( cumStart + freq ) ) / totalFreq - 1;
        low  = low + ( range * cumStart ) / totalFreq;

        while ( true )  {

            if ( high < HALF )  {
                // nada a fazer, apenas desloca abaixo
            }
            else if ( low >= HALF )  {
                low   -= HALF;
                high  -= HALF;
                value -= HALF;
            }
            else if ( low >= QUARTER && high < THREE_QRTR )  {
                low   -= QUARTER;
                high  -= QUARTER;
                value -= QUARTER;
            }
            else  {
                break;
            }

            low   = ( low   << 1 ) & TOP;
            high  = ( ( high << 1 ) & TOP ) | 1;
            value = ( ( value << 1 ) | nextBit() ) & TOP;
        }
    }
}