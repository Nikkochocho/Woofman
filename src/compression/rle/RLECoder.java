package compression.rle;

import java.io.*;

import compression.CompressionAlgorithm;


public class RLECoder implements CompressionAlgorithm  {

    private static final int  MIN_RUN       = 5;     
    private static final int  MAX_BLOCK_LEN = 65535;  
    private static final byte FLAG_LITERAL  = 0;
    private static final byte FLAG_RUN      = 1;

    @Override
    public byte[] compress( byte[] data ) throws IOException  {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream( baos );

        ByteArrayOutputStream literalBuffer = new ByteArrayOutputStream();

        int i = 0;

        while ( i < data.length )  {

            byte currentByte = data[i];
            int  runLength   = 1;

            while ( i + runLength < data.length
                    && data[i + runLength] == currentByte
                    && runLength < MAX_BLOCK_LEN )  {
                runLength++;
            }

            if ( runLength >= MIN_RUN )  {

                flushLiteralBlock( dos, literalBuffer );

                dos.writeByte( FLAG_RUN );
                dos.writeShort( runLength );
                dos.writeByte( currentByte );

                i += runLength;
            }
            else  {

                for ( int k = 0; k < runLength; k++ )  {
                    literalBuffer.write( currentByte );

                    if ( literalBuffer.size() >= MAX_BLOCK_LEN )  {
                        flushLiteralBlock( dos, literalBuffer );
                    }
                }

                i += runLength;
            }
        }

        flushLiteralBlock( dos, literalBuffer ); 

        dos.flush();
        return baos.toByteArray();
    }

    private void flushLiteralBlock( DataOutputStream dos, ByteArrayOutputStream literalBuffer ) throws IOException  {

        if ( literalBuffer.size() == 0 )  {
            return;
        }

        byte[] bytes = literalBuffer.toByteArray();

        dos.writeByte( FLAG_LITERAL );
        dos.writeShort( bytes.length );
        dos.write( bytes );

        literalBuffer.reset();
    }

    @Override
    public byte[] decompress( byte[] compressedData ) throws IOException  {

        DataInputStream dis = new DataInputStream( new ByteArrayInputStream( compressedData ) );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        while ( dis.available() > 0 )  {

            byte flag = dis.readByte();

            if ( flag == FLAG_LITERAL )  {

                int    length = dis.readUnsignedShort();
                byte[] buffer = new byte[ length ];
                dis.readFully( buffer );
                baos.write( buffer );
            }
            else  {

                int  runLength = dis.readUnsignedShort();
                byte value     = dis.readByte();

                for ( int k = 0; k < runLength; k++ )  {
                    baos.write( value );
                }
            }
        }

        return baos.toByteArray();
    }
}