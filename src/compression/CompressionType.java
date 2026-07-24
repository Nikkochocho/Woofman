package compression;

public enum CompressionType  {

    HUFFMAN( (byte) 0 ),
    LZ77_HUFFMAN( (byte) 1 ),
    RLE( (byte) 2 ),
    LZ77_ONLY( (byte) 3 );

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