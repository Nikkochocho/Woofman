package compression.entropy.model;


public class ArrayFrequencyModel implements SymbolModel  {

    private final int[] freq;
    private final int[] cumFreq;
    private final int   alphabetSize;

    public ArrayFrequencyModel( int[] freq )  {

        this.alphabetSize = freq.length;
        this.freq         = freq;
        this.cumFreq       = new int[ alphabetSize + 1 ];

        for ( int i = 0; i < alphabetSize; i++ )  {
            cumFreq[i + 1] = cumFreq[i] + freq[i];
        }
    }

    @Override public int totalFreq()  { return cumFreq[ alphabetSize ]; }

    @Override public int cumStart( int symbol )  { return cumFreq[ symbol ]; }
    @Override public int freqOf( int symbol )    { return freq[ symbol ]; }

    @Override public int symbolAt( int index )   { return index; }
    @Override public int cumStartAt( int index ) { return cumFreq[ index ]; }
    @Override public int freqAt( int index )     { return freq[ index ]; }

    @Override
    public int findIndex( int value )  {

        int lo = 0;
        int hi = alphabetSize - 1;

        while ( lo < hi )  {
            int mid = ( lo + hi + 1 ) >>> 1;
            if ( cumFreq[mid] <= value )  lo = mid;
            else                          hi = mid - 1;
        }

        return lo;
    }
}