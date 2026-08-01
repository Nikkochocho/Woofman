package compression.entropy.model;

import java.util.*;


public class FrequencyModel implements SymbolModel  {

    private final List<Integer>         symbols;
    private final int[]                 cumFreq;
    private final Map<Integer, Integer> indexOf   = new HashMap<>();

    private final int totalFreq;

    public static final int MAX_TOTAL = 1 << 15;   

    public FrequencyModel( Map<Integer, Integer> freq )  {
        
        symbols = new ArrayList<>( freq.keySet() );
        Collections.sort( symbols );

        cumFreq = new int[ symbols.size() + 1 ];
        for ( int i = 0; i < symbols.size(); i++ )  {
            cumFreq[i + 1] = cumFreq[i] + freq.get( symbols.get(i) );
            indexOf.put( symbols.get(i), i );
        }
        totalFreq = cumFreq[ symbols.size() ];
    }

    @Override public int totalFreq()  { return totalFreq; }  

    @Override public int cumStart( int symbol )  { return cumFreq[ indexOf.get( symbol ) ]; }
    @Override public int freqOf( int symbol )    { int i = indexOf.get( symbol ); return cumFreq[i + 1] - cumFreq[i]; }
    @Override public int symbolAt( int index )   { return symbols.get( index ); }
    @Override public int cumStartAt( int index ) { return cumFreq[ index ]; }
    @Override public int freqAt( int index )     { return cumFreq[index + 1] - cumFreq[index]; }

    public int findIndex( int value )  {

        int lo = 0, hi = symbols.size() - 1;
        while ( lo < hi )  {
            int mid = ( lo + hi + 1 ) / 2;
            if ( cumFreq[mid] <= value )  lo = mid; else hi = mid - 1;
        }
        return lo;
    }

    public static Map<Integer, Integer> scale( Map<Integer, Integer> raw )  {

        long sum = 0;
        for ( int f : raw.values() )  sum += f;

        if ( sum <= MAX_TOTAL )  return raw;

        Map<Integer, Integer> scaled    = new LinkedHashMap<>();
        long                  scaledSum = 0;

        for ( Map.Entry<Integer, Integer> entry : raw.entrySet() )  {
            int value = (int) Math.max( 1, ( entry.getValue() * (long) MAX_TOTAL ) / sum );
            scaled.put( entry.getKey(), value );
            scaledSum += value;
        }

        while ( scaledSum > MAX_TOTAL )  {              
            int maxKey = -1, maxVal = 1;
            for ( Map.Entry<Integer, Integer> entry : scaled.entrySet() )  {
                if ( entry.getValue() > maxVal )  { maxVal = entry.getValue(); maxKey = entry.getKey(); }
            }
            if ( maxKey == -1 )  break;
            scaled.put( maxKey, maxVal - 1 );
            scaledSum--;
        }

        return scaled;
    }
}