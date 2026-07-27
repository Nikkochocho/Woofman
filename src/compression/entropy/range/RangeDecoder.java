package compression.entropy.range;

import java.io.DataInputStream;
import java.io.IOException;


public class RangeDecoder  {

    private final DataInputStream in;

    private static final long TOP  = 1L << 24;
    private static final long BOT  = 1L << 16;
    private static final long MASK = 0xFFFFFFFFL;

    private long low   = 0;
    private long range = MASK;
    private long code  = 0;

    private int nextByte() throws IOException  {

        return in.available() > 0 ? in.readUnsignedByte() : 0;
    }

    public RangeDecoder( DataInputStream in ) throws IOException  {

        this.in = in;
        for ( int i = 0; i < 4; i++ )  {
            code = ( ( code << 8 ) | nextByte() ) & MASK;
        }
    }

    public int getFreqValue( int totalFreq )  {

        long r = range / totalFreq;
        return (int) Math.min( totalFreq - 1, ( code - low ) / r );
    }

    public void decode( int cumStart, int freq, int totalFreq ) throws IOException  {

        long r = range / totalFreq;
        low   = ( low + r * cumStart ) & MASK;
        range = r * freq;

        while ( true )  {
            if ( ( ( low ^ ( low + range ) ) & MASK ) < TOP )  {
                // top byte already decided
            }
            else if ( range < BOT )  {
                range = ( -low ) & ( BOT - 1 );
            }
            else  {
                break;
            }
            code  = ( ( code << 8 ) | nextByte() ) & MASK;
            low   = ( low << 8 ) & MASK;
            range = ( range << 8 ) & MASK;
        }
    }
}