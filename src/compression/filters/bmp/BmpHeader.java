package compression.filters.bmp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class BmpHeader  {

    public final int width;
    public final int height;
    public final int bitsPerPixel;
    public final int bytesPerPixel;
    public final int dataOffset;
    public final int rowStride;
    public final int pixelRowBytes;
    public final int dataLength;
    public final int compression;

    private BmpHeader( int width, int height, int bitsPerPixel, int dataOffset, int rowStride, int compression )  {

        this.width         = width;
        this.height        = height;
        this.bitsPerPixel  = bitsPerPixel;
        this.bytesPerPixel = bitsPerPixel / 8;
        this.dataOffset    = dataOffset;
        this.rowStride     = rowStride;
        this.pixelRowBytes = width * this.bytesPerPixel;
        this.dataLength    = rowStride * height;
        this.compression   = compression;
    }

    public static BmpHeader parse( byte[] data )  {

        ByteBuffer buffer = ByteBuffer.wrap( data ).order( ByteOrder.LITTLE_ENDIAN );

        if ( data[0] != 'B' || data[1] != 'M' )  {
            throw new IllegalArgumentException( "Not a valid BMP file (missing BM signature)" );
        }

        int dataOffset   = buffer.getInt( 10 );
        int width        = buffer.getInt( 18 );
        int height       = Math.abs( buffer.getInt( 22 ) );
        int bitsPerPixel = buffer.getShort( 28 ) & 0xFFFF;
        int compression  = buffer.getInt( 30 );

        if ( compression != 0 && compression != 3 && compression != 1 )  {  // 0 = BI_RGB, 3 = BI_BITFIELDS, 1 = BI_RLE8
            throw new IllegalArgumentException( "Unsupported BMP compression: only BI_RGB, BI_BITFIELDS and BI_RLE8 are supported" );
        }
        if ( compression != 1 && bitsPerPixel % 8 != 0 )  {  
            throw new IllegalArgumentException( "Only byte-aligned bit depths are supported (8/24/32 bpp)" );
        }

        int bytesPerPixel = bitsPerPixel / 8;
        int rowStride     = ( ( width * bytesPerPixel + 3 ) / 4 ) * 4; // padded to 4 bytes

        return new BmpHeader( width, height, bitsPerPixel, dataOffset, rowStride, compression );
    }
}