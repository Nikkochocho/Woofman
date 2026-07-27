package compression.entropy.range;

import java.io.DataOutputStream;
import java.io.IOException;


public class RangeEncoder  {

    private final DataOutputStream out;

    private static final long TOP  = 1L << 24;
    private static final long BOT  = 1L << 16;
    private static final long MASK = 0xFFFFFFFFL;

    private long low   = 0;
    private long range = MASK;

    public RangeEncoder( DataOutputStream out )  {

        this.out = out;
    }

    public void encode( int cumStart, int freq, int totalFreq ) throws IOException  {

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
            out.write( (int) ( ( low >>> 24 ) & 0xFF ) );
            low   = ( low << 8 ) & MASK;
            range = ( range << 8 ) & MASK;
        }
    }

    public void finish() throws IOException  {
        
        for ( int i = 0; i < 4; i++ )  {
            out.write( (int) ( ( low >>> 24 ) & 0xFF ) );
            low = ( low << 8 ) & MASK;
        }
    }
}