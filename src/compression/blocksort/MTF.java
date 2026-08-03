package compression.blocksort;


public final class MTF  {

    private static final int ALPHABET_SIZE = 256;

    private MTF()  {}

    private static byte[] initialTable()  {

        byte[] table = new byte[ ALPHABET_SIZE ];
        for ( int i = 0; i < ALPHABET_SIZE; i++ )  table[i] = (byte) i;
        return table;
    }

    private static int indexOf( byte[] table, int symbol )  {

        for ( int i = 0; i < table.length; i++ )  {
            if ( ( table[i] & 0xFF ) == symbol )  return i;
        }

        throw new IllegalStateException( "Símbolo não encontrado na tabela MTF" );   
    }

    private static void moveToFront( byte[] table, int position )  {

        byte symbol = table[position];
        System.arraycopy( table, 0, table, 1, position );
        table[0] = symbol;
    }

    public static byte[] encode( byte[] data )  {

        byte[] table = initialTable();
        byte[] output = new byte[ data.length ];

        for ( int i = 0; i < data.length; i++ )  {

            int symbol   = data[i] & 0xFF;
            int position = indexOf( table, symbol );

            output[i] = (byte) position;
            moveToFront( table, position );
        }

        return output;
    }

    public static byte[] decode( byte[] codes )  {

        byte[] table  = initialTable();
        byte[] output = new byte[ codes.length ];

        for ( int i = 0; i < codes.length; i++ )  {

            int position = codes[i] & 0xFF;
            int symbol   = table[position] & 0xFF;

            output[i] = (byte) symbol;
            moveToFront( table, position );
        }

        return output;
    }
}