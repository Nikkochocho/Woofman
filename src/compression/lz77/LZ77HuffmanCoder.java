// package compression.lz77;

// import java.io.IOException;
// import java.util.List;

// import compression.CompressionAlgorithm;


// public class LZ77HuffmanCoder implements CompressionAlgorithm  {

//     private final LZ77Coder lz77 = new LZ77Coder();

//     @Override
//     public byte[] compress( byte[] data ) throws IOException  {
//         List<LZ77Token> tokens = lz77.tokenize( data );
//         // serializar tokens: separar em símbolos (literais+comprimentos) e distâncias
//         // rodar Huffman (via BTree, igual já existe) em cada fluxo
//         // escrever: [árvore de símbolos][árvore de distâncias][bits codificados]
//     }

//     @Override
//     public byte[] decompress( byte[] compressedData ) throws IOException  {
//         // ler as duas árvores Huffman
//         // decodificar bits de volta pra tokens (usando as árvores, navegação bit a bit)
//         // lz77.detokenize(tokens) reconstrói os bytes originais
//     }
// }