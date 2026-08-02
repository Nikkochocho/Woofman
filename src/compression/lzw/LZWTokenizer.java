package compression.lzw;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class LZWTokenizer  {

    static final int MAX_CODE_BITS = 12;
    static final int DICT_SIZE     = 1 << MAX_CODE_BITS;
    static final int CLEAR_CODE    = 256;                   // Resets dictionary
    static final int FIRST_DYNAMIC = 257;

    private static final class LongIntMap  {

        private static final long EMPTY_KEY = -1L;
        private static final long MULTIPLIER = 0x9E3779B97F4A7C15L;   // Fibonacci hashing

        private final long[] keys;
        private final int[]  values;
        private final int    mask;

        LongIntMap( int capacity )  {                  
            keys   = new long[ capacity ];
            values = new int[ capacity ];
            mask   = capacity - 1;
            clear();
        }

        void clear()  {
            Arrays.fill( keys, EMPTY_KEY );
        }

        private int slotFor( long key )  {
            return (int) ( ( key * MULTIPLIER ) >>> 40 ) & mask;
        }

        int get( long key )  {                         

            int slot = slotFor( key );

            while ( keys[slot] != EMPTY_KEY )  {
                if ( keys[slot] == key )  return values[slot];
                slot = ( slot + 1 ) & mask;
            }
            return -1;
        }

        void put( long key, int value )  {

            int slot = slotFor( key );

            while ( keys[slot] != EMPTY_KEY )  {
                if ( keys[slot] == key )  { values[slot] = value; return; }
                slot = ( slot + 1 ) & mask;
            }
            keys[slot]   = key;
            values[slot] = value;
        }
    }

    private int expand( int code, int[] prevCode, byte[] lastByte, byte[] scratch )  {

        int idx = scratch.length;

        while ( code >= 256 )  {
            scratch[--idx] = lastByte[code];
            code           = prevCode[code];
        }
        scratch[--idx] = (byte) code;              

        return idx;                                
    }

    public List<Integer> encode( byte[] data )  {

        LongIntMap dictionary = new LongIntMap( 1 << 13 );    // 8192
        int        nextCode   = FIRST_DYNAMIC;

        List<Integer> codes = new ArrayList<>();

        int currentCode = -1;     

        for ( byte raw : data )  {

            int b = raw & 0xFF;

            if ( currentCode == -1 )  {
                currentCode = b;                                 
                continue;
            }

            long key  = ( (long) currentCode << 8 ) | b;
            int  next = dictionary.get( key );

            if ( next != -1 )  {
                currentCode = next;
                continue;
            }

            codes.add( currentCode );

            if ( nextCode < DICT_SIZE )  {
                dictionary.put( key, nextCode++ );
            }
            else  {
                codes.add( CLEAR_CODE );
                dictionary.clear();
                nextCode = FIRST_DYNAMIC;
            }

            currentCode = b;
        }

        if ( currentCode != -1 )  {
            codes.add( currentCode );
        }

        return codes;
    }

    public byte[] decode( List<Integer> codes )  {

        int[]  prevCode = new int[ DICT_SIZE ];
        byte[] lastByte = new byte[ DICT_SIZE ];
        int    nextCode = FIRST_DYNAMIC;

        byte[] scratch = new byte[ DICT_SIZE ];    

        ByteArrayOutputStream output         = new ByteArrayOutputStream();
        int                   prevOutputCode = -1;

        for ( int k : codes )  {

            if ( k == CLEAR_CODE )  {
                nextCode       = FIRST_DYNAMIC;
                prevOutputCode = -1;
                continue;
            }

            byte firstByteOfEntry;

            if ( k < nextCode )  {

                int idx = expand( k, prevCode, lastByte, scratch );
                output.write( scratch, idx, scratch.length - idx );
                firstByteOfEntry = scratch[idx];
            }
            else if ( k == nextCode && prevOutputCode != -1 )  {                     // case KwKwK

                int idx = expand( prevOutputCode, prevCode, lastByte, scratch );
                output.write( scratch, idx, scratch.length - idx );
                output.write( scratch[idx] );
                firstByteOfEntry = scratch[idx];
            }
            else  {
                throw new IllegalStateException( "Invalid LZW code: " + k );
            }

            if ( prevOutputCode != -1 && nextCode < DICT_SIZE )  {
                prevCode[ nextCode ] = prevOutputCode;
                lastByte[ nextCode ] = firstByteOfEntry;
                nextCode++;
            }

            prevOutputCode = k;
        }

        return output.toByteArray();
    }
}