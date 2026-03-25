import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;


public class Encoder  {

    // Magic numbers 
    private static final byte[] MAGIC_HEADER    = { 'W', 'O', 'O','F', '1' };
    private static final byte[] MAGIC_FOOTER    = { '1', 'F', 'O', 'O', 'W' };
    private static final short  VERSION         = 1;
    private static final int    TOC_OFFSET_POS  = 9; // magic(5) + version(2) + fileCount(2)
    
    private final Path source;
    private final Path output;

    private List<Path> collectFiles( Path source ) throws IOException  {

        List<Path> list = new ArrayList<>();

        if ( Files.isRegularFile( source ) )  {
            list.add( source );
            return list;
        }

        Files.walkFileTree( source, new SimpleFileVisitor<>()  {
            @Override
            public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )  {
                list.add( file );
                return FileVisitResult.CONTINUE;
            }
        });

        return list;
    }

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

    private byte[] compressFile( byte[] data ) throws IOException  {

        Map<Byte, Integer> headerTable = new HashMap<>();
        BTree              tree        = new BTree();

        for ( byte b : data )  {
            headerTable.merge( b, 1, Integer :: sum );
        }

        tree.setHeaderTable( headerTable );
        tree.buildTree();

        Map<Byte, String> conversionTable = tree.getConversionTable();
        StringBuilder     sb              = new StringBuilder();

        for ( byte b : data )  {
            sb.append( conversionTable.get( b ) );
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream      dos  = new DataOutputStream( baos );

        dos.writeShort( headerTable.size() );

        for ( Map.Entry<Byte, Integer> entry : headerTable.entrySet() )  {
            dos.writeByte( entry.getKey() );
            dos.writeInt( entry.getValue() );
        }

        dos.writeInt( sb.length() );

        int currentByte = 0;
        int bitCount    = 0;

        for ( int i = 0; i < sb.length(); i++ )  {
            currentByte = ( currentByte << 1 ) | ( sb.charAt(i) == '1' ? 1 : 0 );
            if ( ++bitCount == 8 )  {
                dos.write( currentByte );
                currentByte = 0;
                bitCount    = 0;
            }
        }
        if ( bitCount > 0 )  {
            dos.write( currentByte << ( 8 - bitCount ) );
        }

        dos.flush();
        return baos.toByteArray();
    }

    private byte[] sha256( byte[] data )  { 

        try  {
            return MessageDigest.getInstance( "SHA-256" ).digest( data );
        } catch ( NoSuchAlgorithmException e )  {
            throw new RuntimeException( e );
        }
    }

    private void patchOffsetsAndCRC( long tocOffset ) throws IOException  {

        try ( RandomAccessFile raf = new RandomAccessFile( output.toFile(), "rw" ) )  {

            raf.seek( TOC_OFFSET_POS );
            raf.writeLong( tocOffset );

            raf.seek( 0 );

            long   fileLen = raf.length();
            CRC32  crc     = new CRC32();
            byte[] buf     = new byte[ 8192 ];
            long   rem     = fileLen - 4;       // CRC32 pos

            while ( rem > 0 )  {
                int n = raf.read( buf, 0, (int) Math.min( buf.length, rem ) );
                crc.update( buf, 0, n );
                rem -= n;
            }

            raf.seek( fileLen - 4 );
            raf.writeInt( (int) crc.getValue() );
        }
    }

    public Encoder( Path source, Path output )  {

        this.source = source;
        this.output = output;
    }

    public void encode() throws IOException {

        List<Path> files     = collectFiles( source );
        long       tocOffset = 0;

        record Pending( String name, byte[] sha256, long originalSize, byte[] compressed ) {}
        List<Pending> pending = new ArrayList<>();

        for ( Path file : files )  {
            byte[] originalData = readFile( file.toFile() );
            String relativeName = Files.isRegularFile( source )
                ? source.getFileName().toString()
                : source.relativize( file ).toString().replace( '\\', '/' );
            byte[] sha256       = sha256( originalData );                       // might remove
            byte[] compressed   = compressFile( originalData );
            pending.add( new Pending( relativeName, sha256, originalData.length, compressed ) );

            float compressionRate = 100 * ( 1 - ( ( float ) compressed.length / originalData.length ) );
            System.out.println( "Compressed: " + relativeName + " | Compression rate: " + compressionRate + "%" );
        }

        try ( OutputStream os = new BufferedOutputStream( new FileOutputStream( output.toFile() ) ) )  {
        	
        	DataOutputStream dos = new DataOutputStream( os );
            
            dos.write( MAGIC_HEADER );                  // 5 bytes: "WOOF1"
            dos.writeShort( VERSION );                  // 2 bytes (short)
            dos.writeShort( pending.size() );           // 2 bytes (short)
            dos.writeLong( 0L );                        // 8 bytes (long)
            
            // points to the beginning of the file entries
            long cursor = TOC_OFFSET_POS + 8; // HEADER + PLACEHOLDER

            List<long[]> tocEntries = new ArrayList<>(); // Offset
            List<byte[]> tocHashes  = new ArrayList<>(); // SHA-256 hashes

            for ( Pending p : pending )  {
                byte[] nameBytes = p.name().getBytes( java.nio.charset.StandardCharsets.UTF_8 );
                dos.writeShort( nameBytes.length );     // 2 bytes: file name size
                dos.write( nameBytes );                 // N bytes: name UTF-8
                dos.write( p.sha256() );                // 32 bytes: SHA-256
                dos.writeLong( p.originalSize() );      // 8 bytes: original file size - might remove
                dos.writeLong( p.compressed().length ); // 8 bytes: compressed file size

                long metaSize   = 2 + nameBytes.length + 32 + 8 + 8; // FILE NAME + NAME + SHA_256 + OG_SIZE + COMP_SIZE
                long dataOffset = cursor + metaSize;

                tocHashes.add( p.sha256() );
                tocEntries.add( new long[]{ dataOffset } );

                dos.write( p.compressed() );               
                cursor += metaSize + p.compressed().length;
            }

            tocOffset = cursor;
            
            for ( int i = 0; i < tocHashes.size(); i++ )  {
                dos.write( tocHashes.get(i) );           // 32 bytes: SHA-256
                dos.writeLong( tocEntries.get(i)[0] );   // 8 bytes: data offset
            }

            dos.write( MAGIC_FOOTER );   // 5 bytes: "1FOOW"
            dos.writeInt( 0 );           // 4 bytes: placeholder CRC32
            dos.flush();
        }

        patchOffsetsAndCRC( tocOffset );
        System.out.println( "File was created: " + output );
        
    }
}