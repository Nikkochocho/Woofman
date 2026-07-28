package compression.entropy.range;


public class AdaptiveFrequencyModel  {

    private static final int MAX_TOTAL = 1 << 14;   

    private final int   alphabetSize;
    private final int[] tree;                        // Fenwick Tree

    private int lowbit( int i )  { return i & ( -i ); }

    private void update( int i, int delta )  {
        
        for ( ; i <= alphabetSize; i += lowbit(i) )  {
            tree[i] += delta;
        }
    }

    private int prefixSum( int i )  {

        int sum = 0;
        for ( ; i > 0; i -= lowbit(i) )  {
            sum += tree[i];
        }
        return sum;
    }

    private void rescale()  {

        int[] individual = new int[ alphabetSize + 1 ];
        for ( int symbol = 1; symbol <= alphabetSize; symbol++ )  {
            individual[symbol] = Math.max( 1, freqOf( symbol ) / 2 );
        }

        java.util.Arrays.fill( tree, 0 );
        for ( int symbol = 1; symbol <= alphabetSize; symbol++ )  {
            update( symbol, individual[symbol] );
        }
    }

    public AdaptiveFrequencyModel( int alphabetSize )  {

        this.alphabetSize = alphabetSize;
        this.tree         = new int[ alphabetSize + 1 ];

        for ( int symbol = 1; symbol <= alphabetSize; symbol++ )  {
            update( symbol, 1 );                      
        }
    }

    public int totalFreq()  { return prefixSum( alphabetSize ); }

    public int cumStart( int symbol )  { return prefixSum( symbol - 1 ); }
    public int freqOf( int symbol )    { return prefixSum( symbol ) - prefixSum( symbol - 1 ); }

    public int findByCumFreq( int value )  {

        int pos     = 0;
        int remaining = value;
        int bitMask = Integer.highestOneBit( alphabetSize );

        for ( int step = bitMask; step > 0; step >>= 1 )  {
            int next = pos + step;
            if ( next <= alphabetSize && tree[next] <= remaining )  {
                pos        = next;
                remaining -= tree[next];
            }
        }

        return pos + 1;   
    }

    public void increment( int symbol )  {

        update( symbol, 1 );

        if ( totalFreq() > MAX_TOTAL )  {
            rescale();
        }
    }
}