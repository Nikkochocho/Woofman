package compression.lz77;

import java.util.List;


public class LZ77Coder {

    private static final int WINDOW_SIZE     = 32 * 1024;
    private static final int MIN_MATCH       = 3;         
    private static final int MAX_MATCH       = 258;
    
    // public List<LZ77Token> tokenize( byte[] data )  {
    //     // janela deslizante + hash table de prefixos de 3 bytes
    //     // devolve a lista de Literal/Match
    // }

    // public byte[] detokenize( List<LZ77Token> tokens )  {
    //     // percorre os tokens, reconstrói o array de bytes original
    //     // Literal: copia o byte direto
    //     // Match: copia `length` bytes de `distance` posições atrás NO PRÓPRIO ARRAY DE SAÍDA sendo construído
    // }
}
