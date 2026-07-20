package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;


public final class Hashing  {

    private Hashing()  {} 

    public static byte[] sha256( byte[] data )  {

        try  {
            return MessageDigest.getInstance( "SHA-256" ).digest( data );
        } catch ( NoSuchAlgorithmException e )  {
            throw new RuntimeException( e );
        }
    }

    public static long crc32( byte[] data, int offset, int length )  {

        CRC32 crc = new CRC32();
        crc.update( data, offset, length );
        return crc.getValue();
    }
}