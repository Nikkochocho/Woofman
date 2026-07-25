package compression.filters.wav;


public class DeltaFilter  {

    public byte[] apply( byte[] data, WavHeader header )  {

        byte[] filtered = data.clone(); // header + data

        int    channels       = header.numChannels;
        int    bytesPerSample = header.bytesPerSample;
        int    frameSize      = channels * bytesPerSample; 
        int    start          = header.dataOffset;
        int    end            = start + header.dataLength;

        for ( int pos = end - frameSize; pos >= start + frameSize; pos -= frameSize )  {

            for ( int b = 0; b < frameSize; b++ )  {

                int current  = filtered[ pos + b ] & 0xFF;
                int previous = filtered[ pos + b - frameSize ] & 0xFF;

                filtered[ pos + b ] = (byte) ( ( current - previous ) & 0xFF );
            }
        }

        return filtered;
    }

    public byte[] reverse( byte[] filteredData, WavHeader header )  {

        byte[] original = filteredData.clone();

        int channels       = header.numChannels;
        int bytesPerSample = header.bytesPerSample;
        int frameSize      = channels * bytesPerSample;
        int start          = header.dataOffset;
        int end            = start + header.dataLength;

        for ( int pos = start + frameSize; pos < end; pos += frameSize )  {

            for ( int b = 0; b < frameSize; b++ )  {

                int delta    = original[ pos + b ] & 0xFF;
                int previous = original[ pos + b - frameSize ] & 0xFF;

                original[ pos + b ] = (byte) ( ( delta + previous ) & 0xFF );
            }
        }

        return original;
    }
}