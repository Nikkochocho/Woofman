public class Main  {

    public static void main( String[] args )  {

        if ( args.length < 3 )  {
            
            helper.help();
            return;
        }

        String command = args[0].toLowerCase();

        try  {

            switch ( command )  {
                case "encode":
                    helper.encodeProcessing( args[1], args[2] );
                    break;

                case "decode":
                    helper.decodeProcessing( args[1], args[2] );
                    break;

                default:
                    System.out.println( "Invalid command: " + command );
                    helper.help();
            }
        } catch ( Exception e )  {
            System.out.println( "Error during execution: " + e.getMessage() );
        }
    }
}