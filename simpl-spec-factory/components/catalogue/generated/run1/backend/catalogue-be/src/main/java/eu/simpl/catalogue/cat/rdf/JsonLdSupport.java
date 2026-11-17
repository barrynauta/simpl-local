package eu.simpl.catalogue.cat.rdf;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdErrorCode;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import eu.simpl.catalogue.common.Json;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.graph.GraphFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * JSON-LD processing with bundled contexts only (D24). Remote contexts other than the two bundled ones are refused.
 */
public final class JsonLdSupport {
    private static final Map<String, String> BUNDLED = Map.of(
            "https://www.w3.org/2018/credentials/v1", "/contexts/cred-v1.jsonld",
            "https://w3id.org/security/suites/jws-2020/v1", "/contexts/jws-2020-v1.jsonld");
    private static final Map<String, byte[]> CACHE = new HashMap<>();

    public static final DocumentLoader LOADER = new BundledLoader();

    private JsonLdSupport() {}

    /** Expands the whole document to RDF; throws IllegalArgumentException when JSON-LD processing fails. */
    public static void checkExpands(JsonNode doc) {
        try {
            JsonLd.toRdf(document(doc)).loader(LOADER).provide((s, p, o, dt, lang, dir, g) -> null);
        } catch (JsonLdError e) {
            throw new IllegalArgumentException("JSON-LD processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * RDF graph of the credentialSubject (default graph only). Typed numeric @value literals are first turned into
     * their decimal text so that, for example, {"@type":"xsd:decimal","@value":100.5} stays a valid xsd:decimal.
     */
    public static Graph subjectGraph(JsonNode vc) {
        JsonNode subject = normaliseTypedNumbers(vc.path("credentialSubject").deepCopy());
        Graph g = GraphFactory.createDefaultGraph();
        try {
            JsonLd.toRdf(document(subject)).loader(LOADER).provide((s, p, o, dt, lang, dir, graph) -> {
                if (graph == null) g.add(Triple.create(resource(s), NodeFactory.createURI(p), object(o, dt, lang)));
                return null;
            });
        } catch (JsonLdError e) {
            throw new IllegalArgumentException("JSON-LD processing failed: " + e.getMessage(), e);
        }
        return g;
    }

    public static Node resource(String s) {
        return s.startsWith("_:") ? NodeFactory.createBlankNode(s.substring(2)) : NodeFactory.createURI(s);
    }

    private static Node object(String o, String datatype, String lang) {
        if (datatype == null) return resource(o);
        if (lang != null) return NodeFactory.createLiteralLang(o, lang);
        return NodeFactory.createLiteralDT(o, TypeMapper.getInstance().getSafeTypeByName(datatype));
    }

    private static Document document(JsonNode n) throws JsonLdError {
        return JsonDocument.of(new StringReader(Json.write(n)));
    }

    private static JsonNode normaliseTypedNumbers(JsonNode n) {
        if (n.isObject()) {
            ObjectNode o = (ObjectNode) n;
            if (o.has("@value") && o.has("@type") && o.get("@value").isNumber()) {
                o.set("@value", TextNode.valueOf(o.get("@value").decimalValue().toPlainString()));
            }
            Iterator<Map.Entry<String, JsonNode>> it = o.fields();
            while (it.hasNext()) normaliseTypedNumbers(it.next().getValue());
        } else if (n.isArray()) {
            n.forEach(JsonLdSupport::normaliseTypedNumbers);
        }
        return n;
    }

    private static final class BundledLoader implements DocumentLoader {
        @Override
        public Document loadDocument(URI url, DocumentLoaderOptions options) throws JsonLdError {
            String key = url.toString();
            String res = BUNDLED.get(key);
            if (res == null) {
                throw new JsonLdError(JsonLdErrorCode.LOADING_REMOTE_CONTEXT_FAILED,
                        "Remote context " + key + " is not bundled and is never fetched.");
            }
            byte[] bytes;
            synchronized (CACHE) {
                bytes = CACHE.computeIfAbsent(res, r -> {
                    try (InputStream in = JsonLdSupport.class.getResourceAsStream(r)) {
                        return in.readAllBytes();
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                });
            }
            JsonDocument d = JsonDocument.of(new java.io.ByteArrayInputStream(bytes));
            d.setDocumentUrl(url);
            return d;
        }
    }
}
