package compression.blocksort;


public final class BWT  {

    private BWT()  {}

    // Manber–Myers
    private static int[] buildCircularSuffixArray( byte[] data )  {

        int n = data.length;

        int[] sa   = new int[n];
        int[] rank = new int[n];

        for ( int i = 0; i < n; i++ )  {
            sa[i]   = i;
            rank[i] = data[i] & 0xFF;
        }

        int[] secondKey = new int[n];
        int[] tmp       = new int[n];
        int[] newRank   = new int[n];

        for ( int k = 1; k < n; k <<= 1 )  {

            int maxRank = 0;
            for ( int r : rank )  if ( r > maxRank )  maxRank = r;

            for ( int i = 0; i < n; i++ )  secondKey[i] = rank[ ( i + k ) % n ];

            countingSortByKey( sa, secondKey, tmp, maxRank );
            countingSortByKey( tmp, rank, sa, maxRank );

            newRank[ sa[0] ] = 0;
            for ( int i = 1; i < n; i++ )  {

                int prev = sa[i - 1];
                int curr = sa[i];

                boolean same = rank[prev] == rank[curr] && secondKey[prev] == secondKey[curr];
                newRank[curr] = newRank[prev] + ( same ? 0 : 1 );
            }

            System.arraycopy( newRank, 0, rank, 0, n );

            if ( rank[ sa[n - 1] ] == n - 1 )  break;   // todos os ranks distintos: já está totalmente ordenado
        }

        return sa;
    }

    // Counting sort
    private static void countingSortByKey( int[] source, int[] key, int[] dest, int maxRank )  {

        int[] count = new int[ maxRank + 2 ];

        for ( int idx : source )  count[ key[idx] + 1 ]++;
        for ( int i = 0; i < maxRank + 1; i++ )  count[i + 1] += count[i];

        for ( int idx : source )  {
            int k = key[idx];
            dest[ count[k] ] = idx;
            count[k]++;
        }
    }

    public static final class Result  {

        public final byte[] transformed;
        public final int    index;

        public Result( byte[] transformed, int index )  {
            this.transformed = transformed;
            this.index       = index;
        }
    }

    public static Result transform( byte[] data )  {

        int n = data.length;

        if ( n == 0 )  return new Result( new byte[0], 0 );
        if ( n == 1 )  return new Result( data.clone(), 0 );

        int[] sa = buildCircularSuffixArray( data );

        byte[] last  = new byte[n];
        int    index = -1;

        for ( int rank = 0; rank < n; rank++ )  {

            int start = sa[rank];
            last[rank] = data[ ( start - 1 + n ) % n ];

            if ( start == 0 )  index = rank;
        }

        return new Result( last, index );
    }

    public static byte[] inverseTransform( byte[] last, int index )  {

        int n = last.length;

        if ( n == 0 )  return new byte[0];
        if ( n == 1 )  return last.clone();

        int[] count = new int[257];
        for ( byte b : last )  count[ ( b & 0xFF ) + 1 ]++;
        for ( int i = 0; i < 256; i++ )  count[i + 1] += count[i];

        // LF-mapping
        int[] occurrence = new int[256];
        int[] next       = new int[n];

        for ( int i = 0; i < n; i++ )  {

            int symbol = last[i] & 0xFF;
            next[i]    = count[symbol] + occurrence[symbol];
            occurrence[symbol]++;
        }

        byte[] output = new byte[n];
        int    row    = index;

        for ( int i = n - 1; i >= 0; i-- )  {
            output[i] = last[row];
            row       = next[row];
        }

        return output;
    }
}