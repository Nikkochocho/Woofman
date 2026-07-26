package compression.lzw;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import compression.CompressionAlgorithm;
import util.BitWriter;
import util.BitReader;


public class LZWCoder implements CompressionAlgorithm  {

    private static final int MIN_CODE_BITS = 9;
    private static final int MAX_CODE_BITS = LZWTokenizer.MAX_CODE_BITS;
    private static final int DICT_SIZE     = LZWTokenizer.DICT_SIZE;
    private static final int CLEAR_CODE    = LZWTokenizer.CLEAR_CODE;

    private final LZWTokenizer tokenizer = new LZWTokenizer();

    private List<Integer> computeCodeWidths( List<Integer> codes )  {

        List<Integer> widths = new ArrayList<>( codes.size() );

        int nextCode  = LZWTokenizer.FIRST_DYNAMIC;
        int codeWidth = MIN_CODE_BITS;

        for ( int code : codes )  {

            widths.add( codeWidth );

            if ( code == CLEAR_CODE )  {
                nextCode  = LZWTokenizer.FIRST_DYNAMIC;
                codeWidth = MIN_CODE_BITS;
            }
            else if ( nextCode < DICT_SIZE )  {
                nextCode++;
                while ( nextCode >= ( 1 << codeWidth ) && codeWidth < MAX_CODE_BITS )  {
                    codeWidth++;
                }
            }
        }

        return widths;
    }

    @Override
    public byte[] compress( byte[] data ) throws IOException  {

        List<Integer> codes  = tokenizer.encode( data );
        List<Integer> widths = computeCodeWidths( codes );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream( baos );

        dos.writeInt( data.length );
        dos.writeInt( codes.size() );

        BitWriter bitWriter = new BitWriter( dos );
        for ( int i = 0; i < codes.size(); i++ )  {
            bitWriter.writeBits( codes.get(i), widths.get(i) );
        }
        bitWriter.flush();
        dos.flush();

        return baos.toByteArray();
    }

    @Override
    public byte[] decompress( byte[] compressedData ) throws IOException  {

        DataInputStream dis = new DataInputStream( new ByteArrayInputStream( compressedData ) );

        dis.readInt();                       // originalLength (not used)
        int codeCount = dis.readInt();

        BitReader bitReader = new BitReader( dis, dis.available() );

        List<Integer> codes = new ArrayList<>( codeCount );

        int nextCode  = LZWTokenizer.FIRST_DYNAMIC;
        int codeWidth = MIN_CODE_BITS;

        for ( int i = 0; i < codeCount; i++ )  {

            int code = bitReader.readBits( codeWidth );
            codes.add( code );

            if ( code == CLEAR_CODE )  {
                nextCode  = LZWTokenizer.FIRST_DYNAMIC;
                codeWidth = MIN_CODE_BITS;
            }
            else if ( nextCode < DICT_SIZE )  {
                nextCode++;
                while ( nextCode >= ( 1 << codeWidth ) && codeWidth < MAX_CODE_BITS )  {
                    codeWidth++;
                }
            }
        }

        return tokenizer.decode( codes );
    }
}