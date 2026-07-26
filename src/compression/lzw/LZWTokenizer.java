package compression.lzw;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LZWTokenizer  {

    static final int MAX_CODE_BITS = 12;
    static final int DICT_SIZE     = 1 << MAX_CODE_BITS;
    static final int CLEAR_CODE    = 256;             // Resets dictionary
    static final int FIRST_DYNAMIC = 257;

    private Map<String, Integer> freshDictionary()  {

        Map<String, Integer> dictionary = new HashMap<>();
        for ( int i = 0; i < 256; i++ )  {
            dictionary.put( String.valueOf( (char) i ), i );
        }
        return dictionary;
    }

    private Map<Integer, String> freshReverseDictionary()  {

        Map<Integer, String> dictionary = new HashMap<>();
        for ( int i = 0; i < 256; i++ )  {
            dictionary.put( i, String.valueOf( (char) i ) );
        }
        return dictionary;
    }

    private void writeString( ByteArrayOutputStream output, String s )  {

        for ( int i = 0; i < s.length(); i++ )  {
            output.write( s.charAt( i ) & 0xFF );
        }
    }

    public List<Integer> encode( byte[] data )  {

        Map<String, Integer> dictionary = freshDictionary();
        int nextCode = FIRST_DYNAMIC;

        List<Integer> codes = new ArrayList<>();

        String w = "";
        for ( byte b : data )  {

            char   c  = (char) ( b & 0xFF );
            String wc = w + c;

            if ( dictionary.containsKey( wc ) )  {
                w = wc;
                continue;
            }

            codes.add( dictionary.get( w ) );

            if ( nextCode < DICT_SIZE )  {
                dictionary.put( wc, nextCode++ );
            }
            else  {
                codes.add( CLEAR_CODE );
                dictionary = freshDictionary();
                nextCode   = FIRST_DYNAMIC;
            }

            w = String.valueOf( c );
        }
        if ( !w.isEmpty() )  {
            codes.add( dictionary.get( w ) );
        }

        return codes;
    }

    public byte[] decode( List<Integer> codes )  {

        Map<Integer, String> dictionary = freshReverseDictionary();
        int nextCode = FIRST_DYNAMIC;

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String                w      = null;

        for ( int k : codes )  {

            if ( k == CLEAR_CODE )  {
                dictionary = freshReverseDictionary();
                nextCode   = FIRST_DYNAMIC;
                w          = null;
                continue;
            }

            if ( w == null )  {
                w = dictionary.get( k );
                writeString( output, w );
                continue;
            }

            String entry;
            if ( dictionary.containsKey( k ) )  {
                entry = dictionary.get( k );
            }
            else if ( k == nextCode )  {
                entry = w + w.charAt( 0 );
            }
            else  {
                throw new IllegalStateException( "Invalid LZW code: " + k );
            }

            writeString( output, entry );

            if ( nextCode < DICT_SIZE )  {
                dictionary.put( nextCode++, w + entry.charAt( 0 ) );
            }
            w = entry;
        }

        return output.toByteArray();
    }
}