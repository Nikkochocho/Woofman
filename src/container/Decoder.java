package container;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;

import compression.huffman.BNode;
import compression.huffman.BTree;
import util.BitReader;
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

    private byte[] decompressFile( DataInputStream dis, long compressedSize ) throws IOException  {

        long bytesRemaining = compressedSize;

        Map<Byte, Integer> headerTable = new LinkedHashMap<>();
        short tableSize = dis.readShort();
        bytesRemaining -= 2;

        for ( int i = 0; i < tableSize; i++ )  {
            headerTable.put( dis.readByte(), dis.readInt() );
            bytesRemaining -= 5;
        }

        BTree tree = new BTree();
        tree.setHeaderTable( headerTable );
        tree.buildTree();
        BNode root = tree.getRoot();

        int totalBits = dis.readInt();
        bytesRemaining -= 4;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitReader             bitReader = new BitReader( dis, bytesRemaining );
        BNode                 current   = root;

        boolean singleSymbol = root != null && root.getLeft() != null && root.getRight() == null;

        for ( int bitsRead = 0; bitsRead < totalBits; bitsRead++ )  {
            int bit = bitReader.readBit();

            if ( singleSymbol )  {
                baos.write( root.getLeft().getCharacter() );
                continue;
            }

            current = ( bit == 0 ) ? current.getLeft() : current.getRight();

            if ( current.isLeaf() )  {
                baos.write( current.getCharacter() );
                current = root;
            }
        }

        return baos.toByteArray();
    }

    public Decoder( Path input, Path output )  {

        this.input  = input;
        this.output = output;
    }

    public void decode() throws IOException  {

        byte[] data = readFile( input.toFile() );

        verifyMagicAndCRC( data );

        try ( DataInputStream dis = new DataInputStream( new ByteArrayInputStream( data ) ) )  {

            dis.skipBytes( 5 );                 // magic "WOOF1"
            short version   = dis.readShort();
            short fileCount = dis.readShort();
            long  tocOffset = dis.readLong();

            for ( int i = 0; i < fileCount; i++ )  {
                short  nameLen   = dis.readShort();
                byte[] nameBytes = new byte[ nameLen ];
                dis.readFully( nameBytes );
                String name      = new String( nameBytes, java.nio.charset.StandardCharsets.UTF_8 );

                byte[] storedHash      = new byte[ 32 ];
                dis.readFully( storedHash );
                long   originalSize    = dis.readLong();
                long   compressedSize  = dis.readLong();

                FileEntry entry = new FileEntry( name, storedHash, originalSize, compressedSize, 0L, null );

                byte[] decompressed = decompressFile( dis, entry.compressedSize() );
                byte[] actualHash   = Hashing.sha256( decompressed );

                if ( !Arrays.equals( entry.sha256(), actualHash ) )  {
                    throw new IOException( "SHA-256 verification failed: " + entry.name() );
                }

                Path target = output.resolve( entry.name() ).normalize();
                if ( !target.startsWith( output ) )  {
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