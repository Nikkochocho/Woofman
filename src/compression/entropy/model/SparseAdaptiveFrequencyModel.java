package compression.entropy.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;


public class SparseAdaptiveFrequencyModel extends FenwickAdaptiveModel  {

    private final List<Integer>         symbols;
    private final Map<Integer, Integer> symbolIndex;   // symbol -> 0-based index

    private SparseAdaptiveFrequencyModel( List<Integer> sortedSymbols )  {

        super( sortedSymbols.size(), computeMaxTotal( sortedSymbols.size() ) );

        symbols     = sortedSymbols;
        symbolIndex = new HashMap<>();

        for ( int i = 0; i < symbols.size(); i++ )  symbolIndex.put( symbols.get(i), i );
    }

    private static int computeMaxTotal( int size )  {
        return Math.min( FrequencyModel.MAX_TOTAL, Math.max( 1 << 12, size * 8 ) );
    }

    public SparseAdaptiveFrequencyModel( Collection<Integer> distinctSymbols )  {

        this( new ArrayList<>( new TreeSet<>( distinctSymbols ) ) );  
    }

    @Override protected int indexOf( int symbol )  { return symbolIndex.get( symbol ); }

    @Override public int symbolAt( int index )  { return symbols.get( index ); }
}