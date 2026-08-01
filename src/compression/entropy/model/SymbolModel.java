package compression.entropy.model;


public interface SymbolModel  {

    int totalFreq();

    int cumStart( int symbol );
    int freqOf( int symbol );

    int findIndex( int value );
    int symbolAt( int index );
    int cumStartAt( int index );
    int freqAt( int index );

    default void increment( int symbol )  {}
}