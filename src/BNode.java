import java.util.Map;


public class BNode  {

	private byte  character;
    private int   frequency;
    private BNode left, right;

    public BNode ( byte character, int frequencia )  {

        this.character = character;
        this.frequency = frequencia;
        left = right = null;
    }
    
    public int getFrequency()  {
    	
    	return frequency;
    }
    
    public void setLeft( BNode esq )  {
    	
    	this.left = esq;
    }
    
    public void setRight( BNode dir )  {
    	
    	this.right = dir;
    }
    
    public Map<Byte, String> buildConversionTable( String bin, Map<Byte, String> conversionTable )  {
    	
        if ( left == null && right == null )  {
            conversionTable.put( character, bin );
        }
        
        if ( left != null )  {
            left.buildConversionTable( bin + '0', conversionTable );
        }
        if ( right != null )  {
            right.buildConversionTable( bin + '1', conversionTable );
        }
        
        return conversionTable;
    }
    
    // verification methods
    public void showTree()  {

        if ( left != null )  {
            left.showTree();
        }
        if ( right != null )  {
            right.showTree();
        }
        
        System.out.println( frequency ); //postorder 
    }
}