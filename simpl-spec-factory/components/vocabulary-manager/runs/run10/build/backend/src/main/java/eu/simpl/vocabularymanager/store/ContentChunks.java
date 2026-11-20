package eu.simpl.vocabularymanager.store;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.modify.request.QuadAcc;

/**
 * Vocabulary content in the store (D106): the uploaded bytes, base64 encoded, in chunks of at most 1 MB, in a named
 * graph of their own next to the metadata. Bytes come back exactly as uploaded (D29).
 */
public final class ContentChunks {

    static final int CHUNK_BYTES = 1024 * 1024;
    static final int MAX_CHUNKS = 101;

    private ContentChunks() {}

    /** Adds the quads of the content to an insert template. */
    static void addQuads(QuadAcc template, String graphIri, byte[] content) {
        Node graph = NodeFactory.createURI(graphIri);
        int index = 0;
        for (int start = 0; start < content.length || index == 0; start += CHUNK_BYTES) {
            byte[] part = Arrays.copyOfRange(content, start, Math.min(content.length, start + CHUNK_BYTES));
            Node chunk = NodeFactory.createURI(graphIri + ":chunk:" + index);
            template.addQuad(new Quad(graph, chunk, Rdf.RDF_TYPE, Rdf.CHUNK_CLASS));
            template.addQuad(
                    new Quad(graph, chunk, Rdf.CHUNK_INDEX, Rdf.integer(index).asNode()));
            template.addQuad(new Quad(
                    graph,
                    chunk,
                    Rdf.CHUNK_DATA,
                    NodeFactory.createLiteralString(Base64.getEncoder().encodeToString(part))));
            index++;
        }
    }

    /** Joins chunk rows (ordered by index) back into the stored bytes. */
    static byte[] join(List<QuerySolution> rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<QuerySolution> ordered = new ArrayList<>(rows);
        ordered.sort((a, b) -> Integer.compare(Rdf.integer(a, "index"), Rdf.integer(b, "index")));
        for (QuerySolution row : ordered) {
            out.writeBytes(Base64.getDecoder().decode(Rdf.string(row, "data")));
        }
        return out.toByteArray();
    }

    static final String SELECT = Rdf.PREFIXES
            + "SELECT ?index ?data WHERE { GRAPH ?c { ?chunk vm:chunkIndex ?index ; vm:chunkData ?data } }\n"
            + "ORDER BY ?index LIMIT " + MAX_CHUNKS;
}
