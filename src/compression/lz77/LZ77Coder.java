package compression.lz77;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LZ77Coder {

    private static final int WINDOW_SIZE = 32760;
    private static final int MIN_MATCH   = 3;         
    private static final int MAX_MATCH   = 258;
    private static final int HASH_BYTES  = 3;
    
    private int hash3( byte[] data, int pos )  {

        return ( ( data[pos] & 0xFF ) << 16 )
             | ( ( data[pos + 1] & 0xFF ) << 8 )
             | ( data[pos + 2] & 0xFF );
    }

    private void registerPosition( Map<Integer, List<Integer>> positions, byte[] data, int pos )  {

        if ( pos + HASH_BYTES > data.length )  {
            return;
        }

        int hash = hash3( data, pos );
        positions.computeIfAbsent( hash, k -> new ArrayList<>() ).add( pos );
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

        List<LZ77Token>             tokens    = new ArrayList<>();
        Map<Integer, List<Integer>> positions = new HashMap<>();

        int i = 0;

        while ( i < data.length )  {

            int bestLength   = 0;
            int bestDistance = 0;

            if ( i + HASH_BYTES <= data.length )  {

                int hash = hash3( data, i );
                List<Integer> candidates = positions.get( hash );

                if ( candidates != null )  {
                    for ( int j = candidates.size() - 1; j >= 0; j-- )  {

                        int candidatePos = candidates.get( j );

                        if ( i - candidatePos > WINDOW_SIZE )  {
                            break; 
                        }

                        int length = matchLength( data, candidatePos, i );

                        if ( length > bestLength )  {
                            bestLength   = length;
                            bestDistance = i - candidatePos;
                        }
                    }
                }
            }

            if ( bestLength >= MIN_MATCH )  {

                tokens.add( new Match( bestDistance, bestLength ) );

                for ( int k = 0; k < bestLength && i + HASH_BYTES <= data.length; k++ )  {
                    registerPosition( positions, data, i + k );
                }

                i += bestLength;
            }
            else  {

                tokens.add( new Literal( data[i] ) );
                registerPosition( positions, data, i );
                i += 1;
            }
        }

        return tokens;
    }

    public byte[] detokenize( List<LZ77Token> tokens )  {

        byte[] buffer = new byte[ 4096 ]; // capacidade inicial, cresce sob demanda
        int    length = 0;                // quantos bytes já foram escritos de verdade

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
