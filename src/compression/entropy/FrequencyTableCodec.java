package compression.entropy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import util.VarInt;


public final class FrequencyTableCodec  {

    private FrequencyTableCodec()  {}

    public static void writeSparseTable( DataOutputStream dos, Map<Integer, Integer> table ) throws IOException  {  

        List<Integer> symbols = new ArrayList<>( table.keySet() );
        Collections.sort( symbols );

        VarInt.write( dos, symbols.size() );

        int previous = 0;
        for ( int symbol : symbols )  {
            VarInt.write( dos, symbol - previous );
            VarInt.write( dos, table.get( symbol ) );
            previous = symbol;
        }
    }

    public static Map<Integer, Integer> readSparseTable( DataInputStream dis ) throws IOException  {

        Map<Integer, Integer> table = new LinkedHashMap<>();
        int size = VarInt.read( dis );

        int previous = 0;
        for ( int i = 0; i < size; i++ )  {
            int delta  = VarInt.read( dis );
            int symbol = previous + delta;
            int value  = VarInt.read( dis );

            table.put( symbol, value );
            previous = symbol;
        }

        return table;
    }

    public static void writeLengthTable( DataOutputStream dos, Map<Integer, Integer> lengths ) throws IOException  {

        List<Integer> symbols = new ArrayList<>( lengths.keySet() );
        Collections.sort( symbols );

        VarInt.write( dos, symbols.size() );

        int previous = 0;
        for ( int symbol : symbols )  {
            VarInt.write( dos, symbol - previous );
            dos.writeByte( lengths.get( symbol ) );
            previous = symbol;
        }
    }

    public static Map<Integer, Integer> readLengthTable( DataInputStream dis ) throws IOException  {

        Map<Integer, Integer> lengths = new LinkedHashMap<>();
        int size = VarInt.read( dis );

        int previous = 0;
        for ( int i = 0; i < size; i++ )  {
            int delta  = VarInt.read( dis );
            int symbol = previous + delta;
            int length = dis.readUnsignedByte();

            lengths.put( symbol, length );
            previous = symbol;
        }

        return lengths;
    }

    public static void writeSparseAlphabet( DataOutputStream dos, Collection<Integer> distinctSymbols ) throws IOException  {

        List<Integer> symbols = new ArrayList<>( new TreeSet<>( distinctSymbols ) );

        VarInt.write( dos, symbols.size() );

        int previous = 0;
        for ( int symbol : symbols )  {
            VarInt.write( dos, symbol - previous );
            previous = symbol;
        }
    }

    public static List<Integer> readSparseAlphabet( DataInputStream dis ) throws IOException  {

        int size = VarInt.read( dis );
        List<Integer> symbols = new ArrayList<>( size );

        int previous = 0;
        for ( int i = 0; i < size; i++ )  {
            int symbol = previous + VarInt.read( dis );
            symbols.add( symbol );
            previous = symbol;
        }

        return symbols;
    }
}