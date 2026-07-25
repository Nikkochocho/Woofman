package compression.filters.bmp;


public class PaethFilter  {

    private int predictor( int a, int b, int c )  {

        int p  = a + b - c;
        int pa = Math.abs( p - a );
        int pb = Math.abs( p - b );
        int pc = Math.abs( p - c );

        if ( pa <= pb && pa <= pc )  return a;
        if ( pb <= pc )              return b;
        return c;
    }

    public byte[] apply( byte[] data, BmpHeader header )  {

        byte[] filtered = data.clone();

        int bpp    = header.bytesPerPixel;
        int stride = header.rowStride;
        int start  = header.dataOffset;

        for ( int row = header.height - 1; row >= 0; row-- )  {

            int rowStart = start + row * stride;

            for ( int col = header.pixelRowBytes - 1; col >= 0; col-- )  {  

                int idx = rowStart + col;

                int a = col >= bpp ? ( filtered[ idx - bpp ] & 0xFF ) : 0;
                int b = row > 0    ? ( filtered[ idx - stride ] & 0xFF ) : 0;
                int c = ( row > 0 && col >= bpp ) ? ( filtered[ idx - stride - bpp ] & 0xFF ) : 0;

                int predicted = predictor( a, b, c );
                int current   = filtered[ idx ] & 0xFF;

                filtered[ idx ] = (byte) ( ( current - predicted ) & 0xFF );
            }
        }

        return filtered;
    }

    public byte[] reverse( byte[] filteredData, BmpHeader header )  {

        byte[] original = filteredData.clone();

        int bpp    = header.bytesPerPixel;
        int stride = header.rowStride;
        int start  = header.dataOffset;

        for ( int row = 0; row < header.height; row++ )  {

            int rowStart = start + row * stride;

            for ( int col = 0; col < header.pixelRowBytes; col++ )  {  

                int idx = rowStart + col;

                int a = col >= bpp ? ( original[ idx - bpp ] & 0xFF ) : 0;
                int b = row > 0    ? ( original[ idx - stride ] & 0xFF ) : 0;
                int c = ( row > 0 && col >= bpp ) ? ( original[ idx - stride - bpp ] & 0xFF ) : 0;

                int predicted = predictor( a, b, c );
                int delta     = original[ idx ] & 0xFF;

                original[ idx ] = (byte) ( ( delta + predicted ) & 0xFF );
            }
        }

        return original;
    }
}