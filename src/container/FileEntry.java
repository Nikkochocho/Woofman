package container;

import compression.CompressionType;


public record FileEntry(
    String name,
    byte[] sha256,
    long   originalSize,
    long   compressedSize,
    long   dataOffset,
    CompressionType type,
    byte[] compressedData   
) {}