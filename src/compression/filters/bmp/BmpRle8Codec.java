package compression.filters.bmp;

import java.io.ByteArrayOutputStream;


public class BmpRle8Codec  {

    public byte[] decode( byte[] data, BmpHeader header )  {

        byte[] output = new byte[ header.dataLength ]; // rowStride * height

        int pos = header.dataOffset;
        int row = 0;
        int col = 0;

        while ( row < header.height )  {

            int b0 = data[ pos++ ] & 0xFF;
            int b1 = data[ pos++ ] & 0xFF;

            if ( b0 > 0 )  {
                
                for ( int i = 0; i < b0 && col < header.width; i++, col++ )  {
                    output[ row * header.rowStride + col ] = (byte) b1;
                }
            }
            else  {
                switch ( b1 )  {
                    case 0 -> { row++; col = 0; }                  // end line 
                    case 1 -> row = header.height;                 // end bitmap
                    case 2 -> {                                    // delta: jump pixel
                        int dx = data[ pos++ ] & 0xFF;
                        int dy = data[ pos++ ] & 0xFF;
                        col += dx;
                        row += dy;
                    }
                    default -> {                                   // absolute mode: b1 literals
                        int count = b1;
                        for ( int i = 0; i < count; i++, col++ )  {
                            output[ row * header.rowStride + col ] = data[ pos++ ];
                        }
                        if ( ( count % 2 ) == 1 )  pos++;          // padding
                    }
                }
            }
        }

        return output;
    }

    public byte[] encode( byte[] rawGrid, BmpHeader header )  {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for ( int row = 0; row < header.height; row++ )  {

            int col = 0;
            while ( col < header.width )  {

                byte value  = rawGrid[ row * header.rowStride + col ];
                int  runLen = 1;

                while ( col + runLen < header.width
                        && runLen < 255
                        && rawGrid[ row * header.rowStride + col + runLen ] == value )  {
                    runLen++;
                }

                out.write( runLen );
                out.write( value );
                col += runLen;
            }

            out.write( 0 );
            out.write( 0 ); // end line
        }

        out.write( 0 );
        out.write( 1 ); // end bitmap

        return out.toByteArray();
    }
}