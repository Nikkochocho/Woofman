package compression.entropy.model;


public class AdaptiveFrequencyModel extends FenwickAdaptiveModel  {

    private static final int MAX_TOTAL = 1 << 14;

    public AdaptiveFrequencyModel( int alphabetSize )  {
        super( alphabetSize, MAX_TOTAL );
    }

    @Override protected int indexOf( int symbol )  { return symbol; }

    @Override public int symbolAt( int index )  { return index; }
}