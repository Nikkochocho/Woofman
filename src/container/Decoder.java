package container;

import compression.CompressionAlgorithm;
import compression.CompressionType;
import compression.blocksort.BWTCoder;
import compression.entropy.arithmetic.ArithmeticCoder;
import compression.entropy.huffman.HuffmanCoder;
import compression.entropy.range.RangeCoder;
import compression.filters.bmp.BmpPaethHuffmanCoder;
import compression.filters.wav.WavDeltaHuffmanCoder;
import compression.lz77.LZ77ArithmeticCoder;
import compression.lz77.LZ77HuffmanCoder;
import compression.lz77.LZ77OnlyCoder;
import compression.lz77.LZ77RangeCoder;
import compression.lzw.LZWArithmeticCoder;
import compression.lzw.LZWCoder;
import compression.lzw.LZWHuffmanCoder;
import compression.lzw.LZWRangeCoder;
import compression.rle.RLECoder;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.CRC32;
import util.Hashing;


public class Decoder  {
    
    private final Path input;
    private final Path output;

    private byte[] readFile( File file ) throws IOException  {

        int                   read;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];

        try ( InputStream is = new BufferedInputStream( new FileInputStream( file ) ) )  {
            while ( ( read = is.read( buffer ) ) != -1 )  {
                baos.write( buffer, 0, read );
            }
        }

        return baos.toByteArray();
    }

    private void verifyMagicAndCRC( byte[] raw ) throws IOException  {

        int fl = raw.length;

        for ( int i = 0; i < ContainerFormat.MAGIC_HEADER.length; i++ )  {
            if ( raw[i] != ContainerFormat.MAGIC_HEADER[i] )  {
                throw new IOException( "File is not a .woof file (magic header missing!)" );
            }
        }

        int footerStart = fl - 9;
        for ( int i = 0; i < ContainerFormat.MAGIC_FOOTER.length; i++ )  {
            if ( raw[footerStart + i] != ContainerFormat.MAGIC_FOOTER[i] )  {
                throw new IOException( "File is corrupted (magic footer missing)" );
            }
        }

        CRC32 crc = new CRC32();
        crc.update( raw, 0, fl - 4 );
        int stored = ( (raw[fl-4] & 0xFF) << 24 ) | ( (raw[fl-3] & 0xFF) << 16 )
                | ( (raw[fl-2] & 0xFF) <<  8 ) |   (raw[fl-1] & 0xFF);
        if ( (int) crc.getValue() != stored )  {
            throw new IOException( "Invalid CRC32" );
        }
    }

    public Decoder( Path input, Path output )  {

        this.input  = input;
        this.output = output;
    }

    public void decode() throws IOException, Exception  {

        byte[] data = readFile( input.toFile() );

        verifyMagicAndCRC( data );

        try ( DataInputStream dis = new DataInputStream( new ByteArrayInputStream( data ) ) )  {

            dis.skipBytes( 5 );                 // magic "WOOF1"
            short version   = dis.readShort();
            short fileCount = dis.readShort();
            long  tocOffset = dis.readLong();

            boolean singleFile = fileCount == 1;
            Path    parentDir  = output.getParent() != null ? output.getParent() : Path.of( "." );

            for ( int i = 0; i < fileCount; i++ )  {
                short  nameLen   = dis.readShort();
                byte[] nameBytes = new byte[ nameLen ];
                dis.readFully( nameBytes );
                String name      = new String( nameBytes, java.nio.charset.StandardCharsets.UTF_8 );

                byte[] storedHash      = new byte[ 32 ];
                dis.readFully( storedHash );
                long   originalSize    = dis.readLong();
                long   compressedSize  = dis.readLong();
                byte   compressionCode = dis.readByte();          
                CompressionType type   = CompressionType.fromCode( compressionCode );

                byte[] compressedBytes = new byte[ (int) compressedSize ];
                dis.readFully( compressedBytes );

                CompressionAlgorithm algorithm = switch ( type )  {
                    case HUFFMAN         -> new HuffmanCoder();
                    case LZ77_HUFFMAN    -> new LZ77HuffmanCoder();
                    case RLE             -> new RLECoder();
                    case LZ77_ONLY       -> new LZ77OnlyCoder();
                    case DELTA_HUFFMAN   -> new WavDeltaHuffmanCoder();
                    case PAETH_HUFFMAN   -> new BmpPaethHuffmanCoder(); 
                    case LZW             -> new LZWCoder();  
                    case LZW_HUFFMAN     -> new LZWHuffmanCoder();   
                    case RANGE           -> new RangeCoder();  
                    case LZ77_RANGE      -> new LZ77RangeCoder();
                    case LZW_RANGE       -> new LZWRangeCoder();
                    case ARITHMETIC      -> new ArithmeticCoder();
                    case LZ77_ARITHMETIC -> new LZ77ArithmeticCoder();
                    case LZW_ARITHMETIC  -> new LZWArithmeticCoder();
                    case BWT             -> new BWTCoder();
                };

                byte[] decompressed  = algorithm.decompress( compressedBytes );
                byte[] actualHash    = Hashing.sha256( decompressed );

                if ( !Arrays.equals( storedHash, actualHash ) )  {
                    throw new IOException( "SHA-256 verification failed: " + name );
                }

                Path boundary = ( singleFile ? parentDir : output ).toAbsolutePath().normalize();
                Path target   = ( singleFile ? parentDir.resolve( name ) : output.resolve( name ) )
                                .toAbsolutePath().normalize();

                if ( !target.startsWith( boundary ) )  {
                    throw new IOException( "Path traversal detected: " + name );
                }

                Path parent = target.getParent();
                if ( parent != null )  {
                    Files.createDirectories( parent );
                }
                Files.write( target, decompressed );
                System.out.println( "Extracted: " + name + " (" + decompressed.length + " bytes)" );
            }
        }
    }
}