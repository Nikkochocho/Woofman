package util;

import java.io.DataInputStream;
import java.io.IOException;


public class BitReader  {

    private final DataInputStream in;

    private int  currentByte;
    private int  bitMask;
    private long bytesRemaining;

    public BitReader( DataInputStream in, long bytesRemaining )  {

        this.in             = in;
        this.bytesRemaining = bytesRemaining;
        this.bitMask        = 0; 
    }

    public int readBit() throws IOException  {

        if ( bitMask == 0 )  {
            if ( bytesRemaining <= 0 )  {
                throw new IOException( "No more bits to read" );
            }
            currentByte = in.readUnsignedByte();
            bytesRemaining--;
            bitMask = 0x80;
        }

        int bit = ( currentByte & bitMask ) != 0 ? 1 : 0;
        bitMask >>= 1;

        return bit;
    }

    public int readBits( int numBits ) throws IOException  { // used for LZ77

        int value = 0;

        for ( int i = 0; i < numBits; i++ )  {
            value = ( value << 1 ) | readBit();
        }

        return value;
    }
}