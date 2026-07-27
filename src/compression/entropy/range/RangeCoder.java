package compression.entropy.range;

import compression.CompressionAlgorithm;
import compression.entropy.FrequencyTableCodec;
import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;


public class RangeCoder implements CompressionAlgorithm  {

    private static final long TOP       = 1L << 24;
    private static final long BOT       = 1L << 16;
    private static final long MASK      = 0xFFFFFFFFL;
    private static final int  MAX_TOTAL = 1 << 15;      

    private int nextByteOrZero( DataInputStream dis ) throws IOException  {

        return dis.available() > 0 ? dis.readUnsignedByte() : 0;
    }

    private int findSymbol( int[] cumFreq, int value )  {

        for ( int symbol = 0; symbol < 256; symbol++ )  {
            if ( value < cumFreq[ symbol + 1 ] )  return symbol;
        }
        return 255;
    }

    private int[] buildCumulativeFrequencies( int[] freq )  {

        int[] cum = new int[257];
        for ( int i = 0; i < 256; i++ )  {
            cum[i + 1] = cum[i] + freq[i];
        }
        return cum;
    }

    private int[] buildScaledFrequencies( byte[] data )  {

        int[] raw = new int[256];
        for ( byte b : data )  raw[ b & 0xFF ]++;

        long sum = data.length;
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

    @Override
    public byte[] compress( byte[] data ) throws IOException  {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream( baos );

        dos.writeInt( data.length );

        if ( data.length == 0 )  {
            writeFrequencyTable( dos, new int[256] );
            return baos.toByteArray();
        }

        int[] freq    = buildScaledFrequencies( data );
        int[] cumFreq = buildCumulativeFrequencies( freq );
        int   totFreq = cumFreq[256];

        writeFrequencyTable( dos, freq );

        long low   = 0;
        long range = MASK;

        for ( byte b : data )  {

            int  symbol = b & 0xFF;
            long r      = range / totFreq;

            low   = ( low + r * cumFreq[symbol] ) & MASK;
            range = r * freq[symbol];

            while ( true )  {
                if ( ( ( low ^ ( low + range ) ) & MASK ) < TOP )  {
                    // The most significant byte has already been decided.
                }
                else if ( range < BOT )  {
                    range = ( -low ) & ( BOT - 1 );        
                }
                else  {
                    break;
                }
                dos.write( (int) ( ( low >>> 24 ) & 0xFF ) );
                low   = ( low << 8 ) & MASK;
                range = ( range << 8 ) & MASK;
            }
        }

        for ( int i = 0; i < 4; i++ )  {                    
            dos.write( (int) ( ( low >>> 24 ) & 0xFF ) );
            low = ( low << 8 ) & MASK;
        }

        dos.flush();
        return baos.toByteArray();
    }

    @Override
    public byte[] decompress( byte[] compressedData ) throws IOException  {

        DataInputStream dis = new DataInputStream( new ByteArrayInputStream( compressedData ) );

        int originalLength = dis.readInt();
        int[] freq = readFrequencyTable( dis );

        if ( originalLength == 0 )  return new byte[0];

        int[] cumFreq = buildCumulativeFrequencies( freq );
        int   totFreq = cumFreq[256];

        long low   = 0;
        long range = MASK;
        long code  = 0;

        for ( int i = 0; i < 4; i++ )  {
            code = ( ( code << 8 ) | nextByteOrZero( dis ) ) & MASK;
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream( originalLength );

        for ( int n = 0; n < originalLength; n++ )  {

            long r          = range / totFreq;
            int  freqValue  = (int) Math.min( totFreq - 1, ( code - low ) / r );
            int  symbol     = findSymbol( cumFreq, freqValue );

            output.write( symbol );

            low   = ( low + r * cumFreq[symbol] ) & MASK;
            range = r * freq[symbol];

            while ( true )  {
                if ( ( ( low ^ ( low + range ) ) & MASK ) < TOP )  {
                    // The most significant byte has already been decided.
                }
                else if ( range < BOT )  {
                    range = ( -low ) & ( BOT - 1 );
                }
                else  {
                    break;
                }
                code  = ( ( code << 8 ) | nextByteOrZero( dis ) ) & MASK;
                low   = ( low << 8 ) & MASK;
                range = ( range << 8 ) & MASK;
            }
        }

        return output.toByteArray();
    }
}