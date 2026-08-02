package compression.entropy.model;

import java.util.Map;


public final class ModelSelector  {

    private static final double LOG2 = Math.log( 2 );   

    private ModelSelector()  {}

    public static boolean shouldUseAdaptive( Map<Integer, Integer> freq, int totalSymbols, int staticHeaderBytes, int adaptiveHeaderBytes )  {

        int total = 0;
        for ( int f : freq.values() )  total += f;

        double entropyBits = 0;                         
        for ( int f : freq.values() )  {
            entropyBits += f * ( Math.log( (double) total / f ) / LOG2 );  
        }

        int    distinctSymbols        = freq.size();
        double adaptiveRedundancyBits = ( ( distinctSymbols - 1 ) / 2.0 ) * ( Math.log( totalSymbols ) / LOG2 ); 

        double staticCostBits   = entropyBits + staticHeaderBytes   * 8.0;
        double adaptiveCostBits = entropyBits + adaptiveRedundancyBits + adaptiveHeaderBytes * 8.0;

        return adaptiveCostBits < staticCostBits;
    }
}