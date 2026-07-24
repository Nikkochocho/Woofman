package cli;


public class Main  {

    public static void main( String[] args )  {

        if ( args.length < 2 )  {
            Helper.help();
            return;
        }

        String command = args[0];
        String path    = args[1];

        switch ( command )  {
            case "encode" -> Helper.encodeProcessing( path );
            case "decode" -> Helper.decodeProcessing( path );
            default        -> Helper.help();
        }
    }
}