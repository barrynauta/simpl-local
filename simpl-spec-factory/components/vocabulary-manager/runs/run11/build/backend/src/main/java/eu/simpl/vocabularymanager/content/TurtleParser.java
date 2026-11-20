package eu.simpl.vocabularymanager.content;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.RiotParseException;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;
import org.springframework.stereotype.Component;

/**
 * Syntax validation (D9): parses Turtle (RDF 1.1) with Apache Jena RIOT (REUSE-02). Relative IRIs
 * resolve against the given base (D57, D124). Parser warnings do not block; the first error stops
 * the parse.
 */
@Component
public class TurtleParser {

    /**
     * Parses the bytes.
     *
     * @throws TurtleSyntaxException on the first syntax error
     */
    public TurtleDocument parse(byte[] content, String base) throws TurtleSyntaxException {
        Collector collector = new Collector();
        try {
            RDFParser.create()
                    .source(new ByteArrayInputStream(content))
                    .lang(Lang.TURTLE)
                    .base(base)
                    .errorHandler(new FailOnError())
                    .parse(collector);
        } catch (RiotParseException ex) {
            throw new TurtleSyntaxException(ex.getOriginalMessage(), ex.getLine(), ex.getCol());
        } catch (RiotException ex) {
            throw new TurtleSyntaxException(String.valueOf(ex.getMessage()), -1, -1);
        }
        return new TurtleDocument(collector.triples, collector.prefixes);
    }

    private static final class FailOnError implements ErrorHandler {
        @Override
        public void warning(String message, long line, long col) {
            // Warnings (for example unusual IRIs) do not block (D9 blocks on errors only).
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

    private static final class Collector implements StreamRDF {
        private final List<Triple> triples = new ArrayList<>();
        private final List<TurtleDocument.PrefixDeclaration> prefixes = new ArrayList<>();
        private int sequence;

        @Override
        public void start() {
            // nothing to prepare
        }

        @Override
        public void triple(Triple triple) {
            triples.add(triple);
            sequence++;
        }

        @Override
        public void quad(Quad quad) {
            triple(quad.asTriple());
        }

        @Override
        public void base(String base) {
            // @base does not declare a vocabulary reference (D12)
        }

        @Override
        public void prefix(String prefix, String iri) {
            prefixes.add(new TurtleDocument.PrefixDeclaration(prefix, iri, sequence));
            sequence++;
        }

        @Override
        public void version(String version) {
            // RDF 1.2 version announcements carry no reference
        }

        @Override
        public void finish() {
            // nothing to release
        }
    }
}
