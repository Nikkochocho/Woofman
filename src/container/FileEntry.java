package container;


public record FileEntry(
    String name,
    byte[] sha256,
    long   originalSize,
    long   compressedSize,
    long   dataOffset,
    byte[] compressedData   
) {}