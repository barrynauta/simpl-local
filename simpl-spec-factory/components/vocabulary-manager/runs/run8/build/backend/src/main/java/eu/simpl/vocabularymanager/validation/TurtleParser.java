package eu.simpl.vocabularymanager.validation;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.atlas.AtlasException;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.RiotParseException;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.sparql.core.Quad;

/**
 * The syntax step of D9: parses Turtle (RDF 1.1) with Jena RIOT (REUSE-02) and keeps the triples and every prefix
 * declaration in file order. The first error stops the parse.
 */
public final class TurtleParser {

    private static final int MAX_MESSAGE = 300;

    private TurtleParser() {}

    /**
     * @param content the uploaded bytes
     * @param base the base IRI for relative IRIs (D57)
     * @return the triples and prefix declarations
     * @throws TurtleSyntaxException on the first syntax error
     */
    public static ParsedContent parse(byte[] content, String base) throws TurtleSyntaxException {
        Collector collector = new Collector();
        try {
            RDFParser.source(new ByteArrayInputStream(content))
                    .lang(Lang.TURTLE)
                    .base(base)
                    .errorHandler(new FailFirst())
                    .parse(collector);
        } catch (RiotParseException e) {
            throw new TurtleSyntaxException(clean(e.getOriginalMessage()), e.getLine(), e.getCol(), e);
        } catch (RiotException | AtlasException e) {
            throw new TurtleSyntaxException(clean(e.getMessage()), -1, -1, e);
        }
        return new ParsedContent(collector.triples, collector.prefixes);
    }

    private static String clean(String message) {
        if (message == null) {
            return "";
        }
        String cleaned = message.replaceAll("\\[line: \\d+, col: \\d+ ?\\]", "").strip();
        cleaned = cleaned.replaceAll("\\p{Cntrl}", " ");
        return cleaned.length() > MAX_MESSAGE ? cleaned.substring(0, MAX_MESSAGE) + "..." : cleaned;
    }

    /** Collects triples and prefix declarations in file order. */
    private static final class Collector extends StreamRDFBase {
        private final List<Triple> triples = new ArrayList<>();
        private final List<PrefixDeclaration> prefixes = new ArrayList<>();

        @Override
        public void triple(Triple triple) {
            triples.add(triple);
        }

        @Override
        public void quad(Quad quad) {
            triples.add(quad.asTriple());
        }

        @Override
        public void prefix(String prefix, String iri) {
            prefixes.add(new PrefixDeclaration(prefix, iri));
        }
    }

    /** Turns every parser error into an exception; warnings are ignored. */
    private static final class FailFirst implements ErrorHandler {
        @Override
        public void warning(String message, long line, long col) {
            // Warnings (for example an ill-formed typed literal) do not block (D9 lists the blocking rules).
        }

        @Override
        public void error(String message, long line, long col) {
            throw new RiotParseException(message, line, col);
        }

        @Override
        public void fatal(String message, long line, long col) {
            throw new RiotParseException(message, line, col);
        }
    }
}
