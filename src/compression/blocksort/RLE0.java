package compression.blocksort;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;


public final class RLE0  {

    private RLE0()  {}

    private static void emitRun( List<Integer> out, int runLength )  {

        int n = runLength;

        while ( n > 0 )  {
            n--;
            out.add( n % 2 == 0 ? RUNA : RUNB );
            n /= 2;
        }
    }

    public static final int RUNA = 0;
    public static final int RUNB = 1;

    public static final int ALPHABET_SIZE = 257;

    public static int[] encode( byte[] mtf )  {

        List<Integer> out = new ArrayList<>();

        int i = 0;
        while ( i < mtf.length )  {

            if ( ( mtf[i] & 0xFF ) == 0 )  {

                int runLength = 0;
                while ( i < mtf.length && ( mtf[i] & 0xFF ) == 0 )  {
                    runLength++;
                    i++;
                }

                emitRun( out, runLength );
            }
            else  {

                out.add( ( mtf[i] & 0xFF ) + 1 );
                i++;
            }
        }

        int[] result = new int[ out.size() ];
        for ( int k = 0; k < result.length; k++ )  result[k] = out.get(k);
        return result;
    }

    public static byte[] decode( int[] symbols )  {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int i = 0;
        while ( i < symbols.length )  {

            int symbol = symbols[i];

            if ( symbol == RUNA || symbol == RUNB )  {

                long runLength = 0;
                long bit       = 1;

                while ( i < symbols.length && ( symbols[i] == RUNA || symbols[i] == RUNB ) )  {

                    int digit = symbols[i] == RUNA ? 0 : 1;
                    runLength += ( digit + 1 ) * bit;
                    bit <<= 1;
                    i++;
                }

                for ( long k = 0; k < runLength; k++ )  baos.write( 0 );
            }
            else  {

                baos.write( symbol - 1 );
                i++;
            }
        }

        return baos.toByteArray();
    }
}