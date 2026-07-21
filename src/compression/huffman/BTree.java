package compression.huffman;

import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.util.PriorityQueue;


public class BTree  {

    private BNode                 root;
    private Map<Integer, Integer> headerTable;
    private Map<Integer, String>  conversionTable;

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

    public BNode getRoot()  {
    
        return root;
    }
    
    public Map<Integer, String> getConversionTable()  {
    	
    	return conversionTable;
    }
    
    public Map<Integer, Integer> getHeaderTable()  {
    	
    	return headerTable;
    }

    public void setHeaderTable( Map<Integer, Integer> frequencia )  {

        headerTable = frequencia;
    }
   
    public void buildTree()  {

        PriorityQueue<BNode> minHeap = new PriorityQueue<>( Math.max( headerTable.size(), 1 ), comparator );

        for ( Integer i : headerTable.keySet() )  {
            BNode node = new BNode( i, headerTable.get( i ) );
            minHeap.add( node );
        }

        if ( minHeap.size() == 1 )  {
            BNode single  = minHeap.poll();
            BNode wrapper = new BNode( (Integer) 0, single.getFrequency() );
            wrapper.setLeft( single );
            root = wrapper;
            buildConversionTable();
            return;
        }

        while ( !minHeap.isEmpty() )  {
            BNode leftNode  = minHeap.poll();
            BNode rightNode = minHeap.poll();

            if ( leftNode != null && rightNode != null )  {
                int   freq    = leftNode.getFrequency() + rightNode.getFrequency();
                BNode newNode = new BNode( ( Integer ) freq, freq );

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

        for ( Integer i : headerTable.keySet() ) {

            System.out.println( "key: " + i + " value:" + headerTable.get( i ) );
        }
    }

    public void showConversionTable()  {

        for ( Integer i : conversionTable.keySet() ) {

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