package compression.lz77;

import java.io.*;
import java.util.List;

import compression.CompressionAlgorithm;
import util.BitWriter;
import util.BitReader;


public class LZ77OnlyCoder implements CompressionAlgorithm  {

    private final LZ77Coder lz77 = new LZ77Coder();

    @Override
    public byte[] compress( byte[] data ) throws IOException  {

        List<LZ77Token> tokens = lz77.tokenize( data );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream( baos );

        dos.writeInt( tokens.size() );

        BitWriter bitWriter = new BitWriter( dos );

        for ( LZ77Token token : tokens )  {
            switch ( token )  {
                case Literal literal ->  {
                    bitWriter.writeBit( 0 );
                    bitWriter.writeBits( literal.value() & 0xFF, 8 );
                }
                case Match match ->  {
                    bitWriter.writeBit( 1 );
                    bitWriter.writeBits( match.distance(), 32 );
                    bitWriter.writeBits( match.length(), 16 );
                }
            }
        }

        bitWriter.flush();
        dos.flush();

        return baos.toByteArray();
    }

    @Override
    public byte[] decompress( byte[] compressedData ) throws IOException  {

        DataInputStream dis = new DataInputStream( new ByteArrayInputStream( compressedData ) );

        int totalTokens = dis.readInt();

        BitReader bitReader = new BitReader( dis, dis.available() );

        List<LZ77Token> tokens = new java.util.ArrayList<>( totalTokens );

        for ( int t = 0; t < totalTokens; t++ )  {

            int flag = bitReader.readBit();

            if ( flag == 0 )  {
                int value = bitReader.readBits( 8 );
                tokens.add( new Literal( (byte) value ) );
            }
            else  {
                int distance = bitReader.readBits( 32 );
                int length   = bitReader.readBits( 16 );
                tokens.add( new Match( distance, length ) );
            }
        }

        return lz77.detokenize( tokens );
    }
}