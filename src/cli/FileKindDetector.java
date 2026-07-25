package cli;

import java.nio.file.Path;


public final class FileKindDetector  {

    private FileKindDetector()  {}

    public static FileKind detect( Path file )  {

        String name = file.getFileName().toString().toLowerCase();

        if ( name.endsWith( ".wav" ) )  return FileKind.WAV;
        if ( name.endsWith( ".bmp" ) )  return FileKind.BMP;
        if ( name.endsWith( ".txt" ) || name.endsWith( ".md" ) || name.endsWith( ".csv" ) )  return FileKind.TEXT;

        return FileKind.UNKNOWN;
    }
}