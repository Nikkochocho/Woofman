package cli;

public class Main  {

    public static void main( String[] args )  {

        if ( args.length < 2 )  {
            
            Helper.help();
            return;
        }

        String command = args[0].toLowerCase();

        try  {

            switch ( command )  {
                case "encode":
                    Helper.encodeProcessing( args[1] );
                    break;

                case "decode":
                    Helper.decodeProcessing( args[1] );
                    break;

                default:
                    System.out.println( "Invalid command: " + command );
                    Helper.help();
            }
        } catch ( Exception e )  {
            System.out.println( "Error during execution: " + e.getMessage() );
        }
    }
}