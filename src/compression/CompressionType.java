package compression;

public enum CompressionType  {

    HUFFMAN( (byte) 0 ),
    LZ77_HUFFMAN( (byte) 1 ),
    RLE( (byte) 2 ),
    LZ77_ONLY( (byte) 3 ),
    DELTA_HUFFMAN( (byte) 4 ),
    PAETH_HUFFMAN( (byte) 5 ),
    LZW( (byte) 6 ),
    LZW_HUFFMAN( (byte) 7 ),
    RANGE( (byte) 8 ),
    LZ77_RANGE( (byte) 9 ),
    LZW_RANGE( (byte) 10 ),
    RANGE_ADAPTIVE( (byte) 11 );    // TEMPORARY

    private final byte code;

    CompressionType( byte code )  {

        this.code = code;
    }

    public byte getCode()  {

        return code;
    }

    public static CompressionType fromCode( byte code )  {

        for ( CompressionType type : values() )  {
            if ( type.code == code )  {
                return type;
            }
        }
        throw new IllegalArgumentException( "Unknown compression type code: " + code );
    }
}