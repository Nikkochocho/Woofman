package compression.entropy.model;

import java.util.*;


public class SparseAdaptiveFrequencyModel implements SymbolModel  {

    private final List<Integer>         symbols;
    private final Map<Integer, Integer> indexOf   = new HashMap<>();   
    private final int[]                 tree;
    private final int                   size;
    private final int                   maxTotal;

    private int lowbit( int i )  { return i & ( -i ); }

    private void update( int i, int delta )  {

        for ( ; i <= size; i += lowbit(i) )  tree[i] += delta;
    }

    private int prefixSum( int i )  {
        
        int sum = 0;
        for ( ; i > 0; i -= lowbit(i) )  sum += tree[i];
        return sum;
    }

    private int freqAtPos( int pos )  { return prefixSum( pos ) - prefixSum( pos - 1 ); }

    private void rescale()  {

        int[] individual = new int[ size + 1 ];
        for ( int pos = 1; pos <= size; pos++ )  {
            individual[pos] = Math.max( 1, freqAtPos( pos ) / 2 );
        }

        Arrays.fill( tree, 0 );
        for ( int pos = 1; pos <= size; pos++ )  update( pos, individual[pos] );
    }

    public SparseAdaptiveFrequencyModel( Collection<Integer> distinctSymbols )  {

        symbols = new ArrayList<>( new TreeSet<>( distinctSymbols ) );   
        size    = symbols.size();
        tree    = new int[ size + 1 ];

        for ( int i = 0; i < size; i++ )  {
            indexOf.put( symbols.get(i), i + 1 );
            update( i + 1, 1 );
        }

        maxTotal = Math.min( FrequencyModel.MAX_TOTAL, Math.max( 1 << 12, size * 8 ) );
    }

    @Override public int totalFreq()  { return prefixSum( size ); }

    @Override public int cumStart( int symbol )  { return prefixSum( indexOf.get( symbol ) - 1 ); }
    @Override public int freqOf( int symbol )    { return freqAtPos( indexOf.get( symbol ) ); }

    @Override public int symbolAt( int index )   { return symbols.get( index ); }
    @Override public int cumStartAt( int index ) { return prefixSum( index ); }
    @Override public int freqAt( int index )     { return freqAtPos( index + 1 ); }

    @Override
    public int findIndex( int value )  {

        int pos       = 0;
        int remaining = value;
        int bitMask   = Integer.highestOneBit( size );

        for ( int step = bitMask; step > 0; step >>= 1 )  {
            int next = pos + step;
            if ( next <= size && tree[next] <= remaining )  {
                pos        = next;
                remaining -= tree[next];
            }
        }

        return pos;   
    }

    @Override
    public void increment( int symbol )  {

        update( indexOf.get( symbol ), 1 );

        if ( totalFreq() > maxTotal )  rescale();
    }
}