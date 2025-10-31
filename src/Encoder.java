import java.io.*;
import java.util.HashMap;
import java.util.Map;


public class Encoder  {

    private BTree                 tree;
    private ByteArrayOutputStream content;
    private String                pathOriginal;
    private String                pathEncoded;

    private Map<Byte, Integer> readFile()  {

        Map<Byte, Integer> headerTable = new HashMap<>();

        byte[] buffer = new byte[4096];
        int read;

        try ( InputStream is = new BufferedInputStream( new FileInputStream( pathOriginal ) ) )  {
            while ( ( read = is.read( buffer ) ) != -1 )  {
                for ( int i = 0; i < read; i++ )  {
                    byte b = buffer[i];
                    content.write( b );
                    headerTable.merge( b, 1, Integer :: sum );
                }
            }
        } catch ( FileNotFoundException e )  {
            System.out.println( "File not found!" );
        } catch( IOException e )  {
            System.out.println( "Error reading from file!" );
        }

        return headerTable;
    }

    private StringBuilder encodeContent()  {

        StringBuilder     sb              = new StringBuilder();
        Map<Byte, String> conversionTable = tree.getConversionTable();
        byte[]            data            = content.toByteArray();
        
        for ( byte b : data )  {
            sb.append( conversionTable.get( b ) );
        }
        
        return sb;
    }

    private void writeBinary( StringBuilder sb )  {

        Map<Byte, Integer> headerTable = tree.getHeaderTable();

        try ( OutputStream os = new BufferedOutputStream( new FileOutputStream( pathEncoded ) ) )  {
        	
        	DataOutputStream dos = new DataOutputStream( os );
        	
            dos.writeShort( headerTable.size() );
            for ( Map.Entry<Byte, Integer> entry : headerTable.entrySet() )  {
            	dos.writeByte( entry.getKey() );
                dos.writeInt( entry.getValue() );
            }
            dos.writeInt( sb.length() );
            
            int currentByte = 0;
            int bitCount    = 0;
            
            for ( int i = 0; i < sb.length(); i++ )  {
            	char bit    = sb.charAt( i );
            	currentByte = ( currentByte << 1 ) | ( bit == '1' ? 1 : 0 );
            	bitCount++;
            	
            	if ( bitCount == 8 )  {
            		dos.write( currentByte );
            		currentByte = 0;
            		bitCount    = 0;
            	}
            }
            
            if ( bitCount > 0 )  {
            	currentByte <<= ( 8 - bitCount );
            	dos.write( currentByte );
            }
        } catch ( FileNotFoundException e )  {
            System.out.println( "File not found!" );
        } catch( IOException e )  {
            System.out.println( "Error writing file!" );
        }
    }

    public Encoder( String original, String encoded )  {

        this.tree     = new BTree();
        this.content  = new ByteArrayOutputStream();
        this.pathEncoded  = encoded;
        this.pathOriginal  = original;
    }

    public void encode()  {

        tree.setHeaderTable( readFile() );
        tree.buildTree();
        StringBuilder sb = encodeContent();
        writeBinary( sb );
    }

    public void checkCompression()  {

        File originalFile = new File( pathOriginal );
        File encodedFile  = new File( pathEncoded );
        long originalSize = 0;
        long encodedSize  = 0;

        if ( originalFile.exists() && originalFile.isFile() )  {
            originalSize = originalFile.length();
            System.out.println( "Original file size: " + originalSize + " bytes" );
        }
        if ( encodedFile.exists() && encodedFile.isFile() )  {
            encodedSize = encodedFile.length();
            System.out.println( "Encoded file size: " + encodedSize + " bytes" );
        }

        float taxaCompressao = 100 * ( 1 - ( ( float ) encodedSize / originalSize ) );
        System.out.println( "Compression rate: " + taxaCompressao + "%" );
    }

    // verification methods
    public void checkTables()  {

        tree.showHeaderTable();
        tree.showConversionTable();
    }

    public void checkTree()  {

        tree.showTree();
    }
}