package compression.entropy.model;


public class AdaptiveFrequencyModel implements SymbolModel  {

    private static final int MAX_TOTAL = 1 << 14;   

    private final int   alphabetSize;
    private final int[] tree;

    private int lowbit( int i )  { return i & ( -i ); }

    private void update( int i, int delta )  {

        for ( ; i <= alphabetSize; i += lowbit(i) )  tree[i] += delta;
    }

    private int prefixSum( int i )  {
        
        int sum = 0;
        for ( ; i > 0; i -= lowbit(i) )  sum += tree[i];
        return sum;
    }

    private int freqAtPos( int pos )  { return prefixSum( pos ) - prefixSum( pos - 1 ); }

    private void rescale()  {

        int[] individual = new int[ alphabetSize + 1 ];
        for ( int pos = 1; pos <= alphabetSize; pos++ )  {
            individual[pos] = Math.max( 1, freqAtPos( pos ) / 2 );
        }

        java.util.Arrays.fill( tree, 0 );
        for ( int pos = 1; pos <= alphabetSize; pos++ )  update( pos, individual[pos] );
    }

    public AdaptiveFrequencyModel( int alphabetSize )  {

        this.alphabetSize = alphabetSize;
        this.tree         = new int[ alphabetSize + 1 ];

        for ( int pos = 1; pos <= alphabetSize; pos++ )  update( pos, 1 );
    }

    @Override public int totalFreq()  { return prefixSum( alphabetSize ); }

    @Override public int cumStart( int symbol )  { return prefixSum( symbol ); }
    @Override public int freqOf( int symbol )    { return freqAtPos( symbol + 1 ); }

    @Override public int symbolAt( int index )   { return index; }
    @Override public int cumStartAt( int index ) { return cumStart( index ); }
    @Override public int freqAt( int index )     { return freqOf( index ); }

    @Override
    public int findIndex( int value )  {

        int pos       = 0;
        int remaining = value;
        int bitMask   = Integer.highestOneBit( alphabetSize );

        for ( int step = bitMask; step > 0; step >>= 1 )  {
            int next = pos + step;
            if ( next <= alphabetSize && tree[next] <= remaining )  {
                pos        = next;
                remaining -= tree[next];
            }
        }

        return pos;
    }

    @Override
    public void increment( int symbol )  {

        update( symbol + 1, 1 );
        if ( totalFreq() > MAX_TOTAL )  rescale();
    }
}