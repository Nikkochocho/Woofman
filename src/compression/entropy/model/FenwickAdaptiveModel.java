package compression.entropy.model;

import java.util.Arrays;


public abstract class FenwickAdaptiveModel implements SymbolModel  {

    private final int[] tree;      // Fenwick tree 1-indexed;
    private final int   size;
    private final int   maxTotal;

    private int total;             // prefixSum(size) cache

    protected abstract int indexOf( int symbol );

    protected FenwickAdaptiveModel( int size, int maxTotal )  {

        this.size     = size;
        this.tree     = new int[ size + 1 ];
        this.maxTotal = maxTotal;

        for ( int pos = 1; pos <= size; pos++ )  update( pos, 1 );

        total = size;                              
    }

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
        int   newTotal   = 0;

        for ( int pos = 1; pos <= size; pos++ )  {
            individual[pos] = Math.max( 1, freqAtPos( pos ) / 2 );
            newTotal       += individual[pos];
        }

        Arrays.fill( tree, 0 );
        for ( int pos = 1; pos <= size; pos++ )  update( pos, individual[pos] );

        total = newTotal;
    }

    @Override public int totalFreq()  { return total; }

    @Override public int cumStart( int symbol )  { return prefixSum( indexOf( symbol ) ); }
    @Override public int freqOf( int symbol )    { return freqAtPos( indexOf( symbol ) + 1 ); }

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

        return pos;   // 0-based, compatible with symbolAt/cumStartAt/freqAt
    }

    @Override
    public void increment( int symbol )  {

        update( indexOf( symbol ) + 1, 1 );
        total++;

        if ( total > maxTotal )  rescale();
    }
}