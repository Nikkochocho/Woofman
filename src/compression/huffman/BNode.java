package compression.huffman;

import java.util.Map;


public class BNode  {

	private int   character;
    private int   frequency;
    private BNode left, right;

    public BNode ( int character, int frequency )  {

        this.character = character;
        this.frequency = frequency;
        
        left = right = null;
    }
    
    public int getFrequency()  {
    	
    	return frequency;
    }

    public BNode getLeft()  {
    
        return left;
    }

    public BNode getRight()  {
        
        return right;
    }

    public int getCharacter()  {
        
        return character;
    }

    public boolean isLeaf()  {
        
        return left == null && right == null;
    }
    
    public void setLeft( BNode left )  {
    	
    	this.left = left;
    }
    
    public void setRight( BNode right )  {
    	
    	this.right = right;
    }
    
    public Map<Integer, String> buildConversionTable( String bin, Map<Integer, String> conversionTable )  {
    	
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