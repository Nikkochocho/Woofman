package compression.lz77;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LZ77Coder {

    private static final int WINDOW_SIZE      = 32760;
    private static final int MIN_MATCH        = 3;         
    private static final int MAX_MATCH        = 258;
    private static final int HASH_BYTES       = 3;
    private static final int MAX_CHAIN_LENGTH = 64;
    private static final int HASH_BITS        = 15;                   
    private static final int HASH_SIZE        = 1 << HASH_BITS;
    
    private final Map<Integer, ArrayDeque<Integer>> positions = new HashMap<>();

    private int hashIndex( byte[] data, int pos )  {

        int h = ( ( data[pos] & 0xFF ) << 16 )
            | ( ( data[pos+1] & 0xFF ) << 8 )
            |   ( data[pos+2] & 0xFF );

        return ( h * 0x9E3779B1 ) >>> ( 32 - HASH_BITS );
    }

    private void registerPosition( int[] head, int[] prev, byte[] data, int pos )  {

        int idx = hashIndex( data, pos );

        prev[ pos ] = head[ idx ]; 
        head[ idx ] = pos;        
    }

    private int matchLength( byte[] data, int candidatePos, int currentPos )  {

        int length = 0;
        int max    = Math.min( MAX_MATCH, data.length - currentPos );

        while ( length < max && data[candidatePos + length] == data[currentPos + length] )  {
            length++;
        }

        return length;
    }

    private byte[] ensureCapacity( byte[] buffer, int requiredLength )  {

        if ( requiredLength <= buffer.length )  {
            return buffer;
        }

        int newCapacity = buffer.length * 2;
        while ( newCapacity < requiredLength )  {
            newCapacity *= 2;
        }

        return Arrays.copyOf( buffer, newCapacity );
    }

    public List<LZ77Token> tokenize( byte[] data )  {

        List<LZ77Token> tokens = new ArrayList<>();

        int[] head = new int[ HASH_SIZE ];
        int[] prev = new int[ data.length ];
        Arrays.fill( head, -1 ); 

        int i = 0;

        while ( i < data.length )  {

            int bestLength   = 0;
            int bestDistance = 0;

            if ( i + HASH_BYTES <= data.length )  {

                int  idx           = hashIndex( data, i );
                int  candidatePos  = head[ idx ];
                int  checked       = 0;

                while ( candidatePos != -1 && checked < MAX_CHAIN_LENGTH )  {

                    if ( i - candidatePos > WINDOW_SIZE )  {
                        break;
                    }

                    int length = matchLength( data, candidatePos, i );

                    if ( length > bestLength )  {
                        bestLength   = length;
                        bestDistance = i - candidatePos;
                    }

                    if ( bestLength >= MAX_MATCH )  {
                        break;
                    }

                    candidatePos = prev[ candidatePos ];
                    checked++;
                }
            }

            if ( bestLength >= MIN_MATCH )  {

                tokens.add( new Match( bestDistance, bestLength ) );

                for ( int k = 0; k < bestLength && i + k + HASH_BYTES <= data.length; k++ )  {
                    registerPosition( head, prev, data, i + k );
                }

                i += bestLength;
            }
            else  {

                tokens.add( new Literal( data[i] ) );

                if ( i + HASH_BYTES <= data.length )  {
                    registerPosition( head, prev, data, i );
                }

                i += 1;
            }
        }

        return tokens;
    }

    public byte[] detokenize( List<LZ77Token> tokens )  {

        byte[] buffer = new byte[ 4096 ]; 
        int    length = 0;                

        for ( LZ77Token token : tokens )  {

            switch ( token )  {

                case Literal literal ->  {
                    buffer = ensureCapacity( buffer, length + 1 );
                    buffer[ length++ ] = literal.value();
                }

                case Match match ->  {
                    buffer = ensureCapacity( buffer, length + match.length() );

                    int start = length - match.distance();

                    for ( int k = 0; k < match.length(); k++ )  {
                        buffer[ length + k ] = buffer[ start + k ];
                    }

                    length += match.length();
                }
            }
        }

        return Arrays.copyOf( buffer, length );
    }
}
