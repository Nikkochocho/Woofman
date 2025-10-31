import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.util.PriorityQueue;


public class BTree  {

    private BNode              root;
    private Map<Byte, Integer> headerTable;
    private Map<Byte, String>  conversionTable;

    private Comparator<BNode> comparator = new Comparator<BNode>()  {

        public int compare( BNode nodeA, BNode nodeB )  {

            return Integer.compare( nodeA.getFrequency(), nodeB.getFrequency() );
        }
    };
    
    private void buildConversionTable()  {
    	
    	if ( root != null )  {
    		conversionTable = root.buildConversionTable( "", conversionTable );
    	}
    }
    
    public BTree()  {

    	root             = null;
        headerTable      = new HashMap<>();
        conversionTable  = new HashMap<>();
    }
    
    public Map<Byte, String> getConversionTable()  {
    	
    	return conversionTable;
    }
    
    public Map<Byte, Integer> getHeaderTable()  {
    	
    	return headerTable;
    }

    public void setHeaderTable( Map<Byte, Integer> frequencia )  {

        headerTable = frequencia;
    }
   
    public void buildTree()  {
    	
    	 PriorityQueue<BNode> minHeap = new PriorityQueue<>( headerTable.size(), comparator );
    	
    	 for ( byte i : headerTable.keySet() )  {
         	BNode node = new BNode( i, headerTable.get( i ) );
    		minHeap.add( node );
    	 }
    	
        while ( !minHeap.isEmpty() )  {
        	BNode leftNode  = minHeap.poll();
        	BNode rightNode = minHeap.poll();
     
        	if ( leftNode != null && rightNode != null )  {
            	int   freq    = leftNode.getFrequency() + rightNode.getFrequency();
            	BNode newNode = new BNode( ( byte ) freq, freq );

                newNode.setLeft( leftNode );
                newNode.setRight( rightNode );
	        	minHeap.add( newNode );
        	}
        	else  {
        		root = leftNode;
        	}
        }
        
        buildConversionTable();
    }

    // verification methods
    public void showHeaderTable()  {

        for ( byte i : headerTable.keySet() ) {

            System.out.println( "key: " + i + " value:" + headerTable.get( i ) );
        }
    }

    public void showConversionTable()  {

        for ( byte i : conversionTable.keySet() ) {

            System.out.println( "key: " + i + " value:" + conversionTable.get( i ) );
        }
    }

    public void showTree()  {

        if ( root != null )  {
            root.showTree();
        }
        else  {
            System.out.println( "Empty Tree." );
        }
    }
}