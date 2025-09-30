import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Decoder  {

    private BTree  tree;
    private String pathDecoded;
    private String pathEncoded;

    private void writeFile( ByteArrayOutputStream baos )  {
    	
    	try ( OutputStream os = new BufferedOutputStream( new FileOutputStream( pathDecoded ) ) )  {
            os.write( baos.toByteArray() );
        } catch ( FileNotFoundException e )  {
            System.out.println( "File not found!" );
        } catch( IOException e )  {
            System.out.println( "Error writing file!" );
        }
    }
    
    private Map<String, Byte> invertConversionTable( Map<Byte, String> conversionTable )  {
    	
    	Map<String, Byte> invertedTable = new HashMap<>();
    	
    	for ( Map.Entry<Byte, String> entry : conversionTable.entrySet() )  {
    		invertedTable.put( entry.getValue(), entry.getKey() );
    	}
    	
    	return invertedTable;
    }

    public Decoder( String encoded, String decoded )  {
    	
        this.tree        = new BTree();
        this.pathEncoded = encoded;
        this.pathDecoded = decoded;
    }

    public void decode()  {
    	
    	Map<Byte, Integer>    headerTable = new HashMap<>();
        ByteArrayOutputStream baos        = new ByteArrayOutputStream();

        try ( InputStream is = new BufferedInputStream( new FileInputStream( pathEncoded ) ) )  {

            DataInputStream dis = new DataInputStream( is );

            short headerSize = dis.readShort();
            for ( short i = 0; i < headerSize; i++ )  {
                byte key   = dis.readByte();
                int  value = dis.readInt();
                headerTable.put( key, value );
            }
            int bodySize = dis.readInt();

            tree.setHeaderTable( headerTable );
            tree.buildTree();
            Map<Byte, String> conversionTable = tree.getConversionTable();  
            Map<String, Byte> invertedTable   = invertConversionTable( conversionTable );
            
            int           bytesRead;
            int           bitsRead   = 0;
            byte[]        buffer     = new byte[4096];
            StringBuilder temp       = new StringBuilder();
            
            while ( ( ( bytesRead = is.read( buffer ) ) != -1 ) && ( bitsRead < bodySize ) )  {
                for ( int i = 0; i < bytesRead && bitsRead < bodySize; i++ )  {
                	int  bitMask   = 0X80;
                	byte byteAtual = buffer[i];
	            	for ( int j = 0; j < 8 && bitsRead < bodySize; j++ )  {
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
            writeFile( baos );
            
        } catch( FileNotFoundException e )  {
            System.out.println( "File not found!" );
        } catch( IOException e )  {
            System.out.println( "Error reading file!" );
        }
    }
    
    public void checkDecompression()  {

        File decodedFile = new File( pathDecoded );
        long decodedSize = 0;
        
        if ( decodedFile.exists() && decodedFile.isFile() )  {
            decodedSize = decodedFile.length();
            System.out.println( "Decoded file size: " + decodedSize + " bytes" );
        }        
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