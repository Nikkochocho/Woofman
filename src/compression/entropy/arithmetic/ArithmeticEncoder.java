package compression.entropy.arithmetic;

import java.io.IOException;
import util.BitWriter;


public class ArithmeticEncoder  {

    // 32-bit unsigned register, emulated with long + mask (mesma técnica do RangeEncoder)
    private static final long TOP         = 0xFFFFFFFFL;
    private static final long HALF        = 0x80000000L;
    private static final long QUARTER     = 0x40000000L;
    private static final long THREE_QRTR  = 0xC0000000L;

    private final BitWriter bitWriter;

    private long low         = 0;
    private long high        = TOP;
    private int  pendingBits = 0;

    public ArithmeticEncoder( BitWriter bitWriter )  {

        this.bitWriter = bitWriter;
    }

    // Emite um bit já decidido e "descarrega" os bits pendentes de underflow (E3),
    // que são sempre o complemento do bit recém decidido.
    private void emit( int bit ) throws IOException  {

        bitWriter.writeBit( bit );

        for ( ; pendingBits > 0; pendingBits-- )  {
            bitWriter.writeBit( bit ^ 1 );
        }
    }

    public void encode( int cumStart, int freq, int totalFreq ) throws IOException  {

        long range = ( high - low + 1 );

        high = low + ( range * ( cumStart + freq ) ) / totalFreq - 1;
        low  = low + ( range * cumStart ) / totalFreq;

        while ( true )  {

            if ( high < HALF )  {                                  // E1: intervalo inteiro na metade baixa
                emit( 0 );
            }
            else if ( low >= HALF )  {                              // E2: intervalo inteiro na metade alta
                emit( 1 );
                low  -= HALF;
                high -= HALF;
            }
            else if ( low >= QUARTER && high < THREE_QRTR )  {      // E3: underflow, intervalo preso no meio
                pendingBits++;
                low  -= QUARTER;
                high -= QUARTER;
            }
            else  {
                break;
            }

            low  = ( low  << 1 ) & TOP;
            high = ( ( high << 1 ) & TOP ) | 1;
        }
    }

    public void finish() throws IOException  {

        pendingBits++;
        emit( low < QUARTER ? 0 : 1 );

        bitWriter.flush();
    }
}