package util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public final class VarInt  {

    private VarInt()  {}

    public static void write( DataOutputStream dos, int value ) throws IOException  {

        while ( ( value & ~0x7F ) != 0 )  {
            dos.writeByte( ( value & 0x7F ) | 0x80 );
            value >>>= 7;
        }
        dos.writeByte( value & 0x7F );
    }

    public static int read( DataInputStream dis ) throws IOException  {

        int value = 0;
        int shift = 0;
        int b;

        do  {
            b = dis.readUnsignedByte();
            value |= ( b & 0x7F ) << shift;
            shift += 7;
        } while ( ( b & 0x80 ) != 0 );

        return value;
    }
}