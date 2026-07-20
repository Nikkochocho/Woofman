package container;

public final class ContainerFormat  {

    public static final byte[] MAGIC_HEADER    = { 'W', 'O', 'O', 'F', '1' };
    public static final byte[] MAGIC_FOOTER    = { '1', 'F', 'O', 'O', 'W' };
    public static final short  VERSION         = 1;
    public static final int    TOC_OFFSET_POS  = 9; 

    private ContainerFormat()  {} 
}