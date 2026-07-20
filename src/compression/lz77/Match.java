package compression.lz77;

public record Match( int distance, int length ) implements LZ77Token {}