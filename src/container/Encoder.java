package container;

import compression.CompressionAlgorithm;
import compression.huffman.HuffmanCoder;
import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import util.Hashing;


public class Encoder  {

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

    private void patchOffsetsAndCRC( long tocOffset ) throws IOException  {

        try ( RandomAccessFile raf = new RandomAccessFile( output.toFile(), "rw" ) )  {

            raf.seek( ContainerFormat.TOC_OFFSET_POS );
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

        List<FileEntry> pending = new ArrayList<>();

        for ( Path file : files )  {
            byte[] originalData = readFile( file.toFile() );
            String relativeName = Files.isRegularFile( source )
                ? source.getFileName().toString()
                : source.relativize( file ).toString().replace( '\\', '/' );
            byte[] sha256       = Hashing.sha256( originalData );

            CompressionAlgorithm algorithm  = new HuffmanCoder();
            byte[]               compressed = algorithm.compress( originalData );

            pending.add( new FileEntry( relativeName, sha256, originalData.length, compressed.length, 0L, compressed ) );

            float compressionRate = 100 * ( 1 - ( ( float ) compressed.length / originalData.length ) );
            System.out.println( "Compressed: " + relativeName + " | Compression rate: " + compressionRate + "%" );
        }

        try ( OutputStream os = new BufferedOutputStream( new FileOutputStream( output.toFile() ) ) )  {

            DataOutputStream dos = new DataOutputStream( os );

            dos.write( ContainerFormat.MAGIC_HEADER );
            dos.writeShort( ContainerFormat.VERSION );
            dos.writeShort( pending.size() );
            dos.writeLong( 0L );

            long cursor = ContainerFormat.TOC_OFFSET_POS + 8;

            List<long[]> tocEntries = new ArrayList<>();
            List<byte[]> tocHashes  = new ArrayList<>();

            for ( FileEntry p : pending )  {
                byte[] nameBytes = p.name().getBytes( java.nio.charset.StandardCharsets.UTF_8 );
                dos.writeShort( nameBytes.length );
                dos.write( nameBytes );
                dos.write( p.sha256() );
                dos.writeLong( p.originalSize() );
                dos.writeLong( p.compressedSize() );

                long metaSize   = 2 + nameBytes.length + 32 + 8 + 8;
                long dataOffset = cursor + metaSize;

                tocHashes.add( p.sha256() );
                tocEntries.add( new long[]{ dataOffset } );

                dos.write( p.compressedData() );
                cursor += metaSize + p.compressedData().length;
            }

            tocOffset = cursor;

            for ( int i = 0; i < tocHashes.size(); i++ )  {
                dos.write( tocHashes.get(i) );
                dos.writeLong( tocEntries.get(i)[0] );
            }

            dos.write( ContainerFormat.MAGIC_FOOTER );
            dos.writeInt( 0 );
            dos.flush();
        }

        patchOffsetsAndCRC( tocOffset );
        System.out.println( "File was created: " + output );
        
    }
}