package eu.simpl.vocabularymanager.store;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.update.UpdateRequest;

/**
 * Content bytes in the store: split into chunks of 48 KiB (64 KiB of base64), one node per chunk in the content graph.
 * One large literal makes the SPARQL parser of the store slow (its token buffer grows step by step); chunks keep a
 * 5 MB write within the budget of D71. The insert is built with Jena's update object model (D94) and runs in the
 * same update request, so in the same store transaction, as the metadata (D8).
 */
public final class ContentChunks {

    /** Raw bytes per chunk; a multiple of 3 so every chunk is valid base64 on its own. */
    static final int CHUNK_BYTES = 48 * 1024;

    /** Upper bound of chunks for the 6 MB request limit, used as the LIMIT of content queries (D85). */
    public static final int MAX_CHUNKS = (6 * 1024 * 1024) / CHUNK_BYTES + 2;

    private static final Node INDEX = NodeFactory.createURI(StoreModel.VM + "index");
    private static final Node DATA = NodeFactory.createURI(StoreModel.VM + "data");
    private static final Node CHUNK_OF = NodeFactory.createURI(StoreModel.VM + "chunkOf");

    private ContentChunks() {}

    /**
     * An update operation that inserts the content chunks when the write token is present.
     *
     * @param graph content graph
     * @param owner node the content belongs to (version or external vocabulary)
     * @param content the bytes as uploaded
     * @param condition a SELECT query whose pattern is the condition of the insert
     * @return the operation as SPARQL text
     */
    public static String insert(String graph, String owner, byte[] content, String condition) {
        Node graphNode = NodeFactory.createURI(graph);
        Node ownerNode = NodeFactory.createURI(owner);
        UpdateModify modify = new UpdateModify();
        int chunks = Math.max(1, (content.length + CHUNK_BYTES - 1) / CHUNK_BYTES);
        for (int i = 0; i < chunks; i++) {
            byte[] part = Arrays.copyOfRange(content, i * CHUNK_BYTES, Math.min(content.length, (i + 1) * CHUNK_BYTES));
            Node chunk = NodeFactory.createURI(owner + ":chunk:" + i);
            modify.getInsertAcc().addQuad(new Quad(graphNode, chunk, CHUNK_OF, ownerNode));
            modify.getInsertAcc()
                    .addQuad(new Quad(
                            graphNode,
                            chunk,
                            INDEX,
                            NodeFactory.createLiteralDT(Integer.toString(i), XSDDatatype.XSDinteger)));
            modify.getInsertAcc()
                    .addQuad(new Quad(
                            graphNode,
                            chunk,
                            DATA,
                            NodeFactory.createLiteralDT(
                                    Base64.getEncoder().encodeToString(part), XSDDatatype.XSDbase64Binary)));
        }
        Query query = QueryFactory.create(condition);
        modify.setElement(query.getQueryPattern());
        modify.setHasInsertClause(true);
        return new UpdateRequest(modify).toString();
    }

    /**
     * Joins chunk values in index order.
     *
     * @param base64Chunks base64 values ordered by index
     * @return the bytes
     */
    public static byte[] join(List<String> base64Chunks) {
        int total = 0;
        byte[][] parts = new byte[base64Chunks.size()][];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = Base64.getDecoder().decode(base64Chunks.get(i));
            total += parts[i].length;
        }
        byte[] result = new byte[total];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }
        return result;
    }
}
