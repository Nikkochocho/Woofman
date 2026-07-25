package compression.filters.wav;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class WavHeader  {

    public final int numChannels;
    public final int bitsPerSample;
    public final int bytesPerSample;
    public final int dataOffset;
    public final int dataLength;

    private WavHeader( int numChannels, int bitsPerSample, int dataOffset, int dataLength )  {

        this.numChannels    = numChannels;
        this.bitsPerSample  = bitsPerSample;
        this.bytesPerSample = bitsPerSample / 8;
        this.dataOffset     = dataOffset;
        this.dataLength     = dataLength;
    }

    public static WavHeader parse( byte[] data )  {

        ByteBuffer buffer = ByteBuffer.wrap( data ).order( ByteOrder.LITTLE_ENDIAN );

        // validates basic signature
        if ( data[0] != 'R' || data[1] != 'I' || data[2] != 'F' || data[3] != 'F' )  {
            throw new IllegalArgumentException( "Not a valid WAV file (missing RIFF header)" );
        }
        if ( data[8] != 'W' || data[9] != 'A' || data[10] != 'V' || data[11] != 'E' )  {
            throw new IllegalArgumentException( "Not a valid WAV file (missing WAVE marker)" );
        }

        int numChannels   = -1;
        int bitsPerSample = -1;
        int dataOffset    = -1;
        int dataLength    = -1;

        int pos = 12; // after "WAVE"

        while ( pos + 8 <= data.length )  {

            String chunkId   = new String( data, pos, 4, java.nio.charset.StandardCharsets.US_ASCII );
            int    chunkSize = buffer.getInt( pos + 4 );

            if ( chunkId.equals( "fmt " ) )  {
                numChannels   = buffer.getShort( pos + 10 ) & 0xFFFF;
                bitsPerSample = buffer.getShort( pos + 22 ) & 0xFFFF;
            }
            else if ( chunkId.equals( "data" ) )  {
                dataOffset = pos + 8;
                dataLength = chunkSize;
                break; 
            }

            pos += 8 + chunkSize + ( chunkSize % 2 ); 
        }

        if ( numChannels == -1 || dataOffset == -1 )  {
            throw new IllegalArgumentException( "Could not find required WAV chunks (fmt/data)" );
        }

        return new WavHeader( numChannels, bitsPerSample, dataOffset, dataLength );
    }
}