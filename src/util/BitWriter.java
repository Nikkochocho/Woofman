package util;

import java.io.IOException;
import java.io.OutputStream;


public class BitWriter  {

    private final OutputStream out;
    private       int          currentByte;
private           int          bitCount;

    public BitWriter( OutputStream out )  {

        this.out         = out;
        this.currentByte = 0;
        this.bitCount    = 0;
    }

    public void writeBit( int bit ) throws IOException  {

        currentByte = ( currentByte << 1 ) | ( bit & 1 );
        bitCount++;

        if ( bitCount == 8 )  {
            out.write( currentByte );
            currentByte = 0;
            bitCount    = 0;
        }
    }

    public void writeBits( String bits ) throws IOException  {

        for ( int i = 0; i < bits.length(); i++ )  {
            writeBit( bits.charAt( i ) == '1' ? 1 : 0 );
        }
    }

    public void writeBits( int value, int numBits ) throws IOException  { //used for LZ77

        for ( int i = numBits - 1; i >= 0; i-- )  {
            writeBit( ( value >> i ) & 1 );
        }
    }

    public void flush() throws IOException  {

        if ( bitCount > 0 )  {
            out.write( currentByte << ( 8 - bitCount ) );
            currentByte = 0;
            bitCount    = 0;
        }
    }
}