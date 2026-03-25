import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;


public class Decoder  {

    private static final byte[] MAGIC_HEADER    = { 'W', 'O', 'O','F', '1' };
    private static final byte[] MAGIC_FOOTER    = { '1', 'F', 'O', 'O', 'W' };
    
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
    
    private Map<String, Byte> invertConversionTable( Map<Byte, String> conversionTable )  {
    	
    	Map<String, Byte> invertedTable = new HashMap<>();
    	
    	for ( Map.Entry<Byte, String> entry : conversionTable.entrySet() )  {
    		invertedTable.put( entry.getValue(), entry.getKey() );
    	}
    	
    	return invertedTable;
    }

    private void verifyMagicAndCRC( byte[] raw ) throws IOException  {

        int fl = raw.length;

        if ( raw[0] != 'W' || raw[1] != 'O' || raw[2] != 'O' || raw[3] != 'F' || raw[4] != '1' )  {
            throw new IOException( "File is not a .woof file (magic header missing!)" );
        }

        if ( raw[fl-9] != '1' || raw[fl-8] != 'F' || raw[fl-7] != 'O' || raw[fl-6] != 'O' || raw[fl-5] != 'W' )  { 
            throw new IOException( "File is corrupted (magic footer missing)" );
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
        Map<String, Byte> invertedTable = invertConversionTable( tree.getConversionTable() );

        int totalBits = dis.readInt();
        bytesRemaining -= 4;

        int                   read;
        byte[]                buffer   = new byte[ 4096 ];
        ByteArrayOutputStream baos     = new ByteArrayOutputStream();
        StringBuilder         temp     = new StringBuilder();
        int                   bitsRead = 0;

        while ( ( read = dis.read( buffer, 0, (int) Math.min( buffer.length, bytesRemaining ) ) ) != -1 && bitsRead < totalBits )  {
            bytesRemaining -= read;
            for ( int i = 0; i < read && bitsRead < totalBits; i++ )  {
                int  bitMask   = 0x80;
                byte byteAtual = buffer[i];
                for ( int j = 0; j < 8 && bitsRead < totalBits; j++ )  {
                    int bit = ( byteAtual & bitMask ) != 0 ? 1 : 0;
                    temp.append( bit );
                    bitsRead++;
                    if ( invertedTable.containsKey( temp.toString() ) )  {
                        baos.write( invertedTable.get( temp.toString() ) );
                        temp.setLength( 0 );
                    }
                    bitMask >>= 1;
                }
            }
        }

        return baos.toByteArray();
    }

    private byte[] sha256( byte[] data )  { 

        try  {
            return MessageDigest.getInstance( "SHA-256" ).digest( data );
        } catch ( NoSuchAlgorithmException e )  {
            throw new RuntimeException( e );
        }
    }

    public Decoder( Path input, Path output )  {

        this.input  = input;
        this.output = output;
    }

    public void decode() throws IOException  {

        byte[] data = readFile( input.toFile() );

        verifyMagicAndCRC( data );

         try ( InputStream is = new BufferedInputStream( new FileInputStream( input.toFile() ) ) )  {

            DataInputStream dis = new DataInputStream( is );

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

                byte[] decompressed = decompressFile( dis, compressedSize );
                byte[] actualHash   = sha256( decompressed );

                if ( !Arrays.equals( storedHash, actualHash ) )  {
                    throw new IOException( "SHA-256 verification failed: " + name );
                }

                Path target = output.resolve( name ).normalize();
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