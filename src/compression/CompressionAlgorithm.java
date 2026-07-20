package compression;

import java.io.IOException;


public interface CompressionAlgorithm  {

    byte[] compress( byte[] data ) throws IOException;

    byte[] decompress( byte[] compressedData ) throws IOException;
}