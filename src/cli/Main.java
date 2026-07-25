package cli;


public class Main  {

    public static void main( String[] args )  {

        if ( args.length < 2 )  {
            CliCommands.help();
            return;
        }

        String command = args[0];
        String path    = args[1];

        switch ( command )  {
            case "encode" -> CliCommands.encodeProcessing( path );
            case "decode" -> CliCommands.decodeProcessing( path );
            default       -> CliCommands.help();
        }
    }
}