package cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;


public final class FileKindDetector  {

    private static final byte[] BMP_MAGIC  = { 'B', 'M' };
    private static final byte[] RIFF_MAGIC = { 'R', 'I', 'F', 'F' };
    private static final byte[] WAVE_MAGIC = { 'W', 'A', 'V', 'E' };

    private static final int SNIFF_SIZE = 512; 

    private FileKindDetector()  {}

    private static byte[] readHeader( Path file, int maxBytes )  {

        try ( InputStream is = Files.newInputStream( file ) )  {

            byte[] buffer = new byte[ maxBytes ];
            int    read   = is.read( buffer );

            if ( read <= 0 )  return new byte[0];

            byte[] header = new byte[ read ];
            System.arraycopy( buffer, 0, header, 0, read );
            return header;
        }
        catch ( IOException ex )  {
            return null; // Unreadable file -> treated as UNKNOWN
        }
    }

    private static boolean isBmp( byte[] header )  {

        return startsWith( header, BMP_MAGIC );
    }

    private static boolean isWav( byte[] header )  {

        // RIFF structure: "RIFF" + size (4 bytes) + subtype
        return header.length >= 12
                && startsWith( header, RIFF_MAGIC )
                && header[8]  == WAVE_MAGIC[0]
                && header[9]  == WAVE_MAGIC[1]
                && header[10] == WAVE_MAGIC[2]
                && header[11] == WAVE_MAGIC[3];
    }

    private static boolean startsWith( byte[] data, byte[] prefix )  {

        if ( data.length < prefix.length )  return false;

        for ( int i = 0; i < prefix.length; i++ )  {
            if ( data[i] != prefix[i] )  return false;
        }
        return true;
    }

    private static boolean looksLikeText( byte[] header )  {

        int printable = 0;

        for ( byte b : header )  {

            int value = b & 0xFF;

            if ( value == 0 )  return false; 

            boolean isControlAllowed   = value == '\t' || value == '\n' || value == '\r';
            boolean isPrintableAscii   = value >= 0x20 && value <= 0x7E;
            boolean isUtf8Continuation = value >= 0x80; 

            if ( isControlAllowed || isPrintableAscii || isUtf8Continuation )  {
                printable++;
            }
        }

        return ( (double) printable / header.length ) >= 0.95;
    }

    public static FileKind detect( Path file )  {

        byte[] header = readHeader( file, SNIFF_SIZE );
        if ( header == null || header.length == 0 )  return FileKind.UNKNOWN;

        if ( isBmp( header ) )         return FileKind.BMP;
        if ( isWav( header ) )         return FileKind.WAV;
        if ( looksLikeText( header ) ) return FileKind.TEXT;

        return FileKind.UNKNOWN;
    }
}