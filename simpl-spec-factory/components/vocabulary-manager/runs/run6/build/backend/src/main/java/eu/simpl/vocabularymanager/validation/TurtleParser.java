package eu.simpl.vocabularymanager.validation;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.RiotParseException;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.riot.system.StreamRDFWrapper;
import org.apache.jena.sparql.graph.GraphFactory;
import org.springframework.stereotype.Component;

/**
 * Parses Turtle (RDF 1.1) with Apache Jena RIOT (REUSE-02) and keeps the prefix declarations, which are the
 * vocabulary references of decision D12. The first error stops parsing (decision D9).
 */
@Component
public class TurtleParser {

    /**
     * A declared prefix.
     *
     * @param prefix the prefix label, empty for the default prefix
     * @param namespace the namespace IRI, resolved against the base
     */
    public record PrefixDeclaration(String prefix, String namespace) {}

    /**
     * Result of a successful parse.
     *
     * @param graph the triples
     * @param prefixes the prefix declarations in order, without exact repeats
     */
    public record Parsed(Graph graph, List<PrefixDeclaration> prefixes) {}

    /** The content is not valid Turtle. */
    public static final class SyntaxError extends Exception {

        private static final long serialVersionUID = 1L;

        private final long line;
        private final long column;

        /**
         * Creates the error.
         *
         * @param message parser message
         * @param line line, 1-based
         * @param column column, 1-based
         */
        public SyntaxError(String message, long line, long column) {
            super(message);
            this.line = line < 1 ? 1 : line;
            this.column = column < 1 ? 1 : column;
        }

        public long line() {
            return line;
        }

        public long column() {
            return column;
        }
    }

    /**
     * Parses the content.
     *
     * @param content the uploaded bytes
     * @param baseIri the base for relative IRIs (decision D57)
     * @return the graph and the prefix declarations
     * @throws SyntaxError on the first syntax error
     */
    public Parsed parse(byte[] content, String baseIri) throws SyntaxError {
        Graph graph = GraphFactory.createDefaultGraph();
        Set<PrefixDeclaration> prefixes = new LinkedHashSet<>();
        StreamRDF sink = new StreamRDFWrapper(StreamRDFLib.graph(graph)) {
            @Override
            public void prefix(String prefix, String iri) {
                prefixes.add(new PrefixDeclaration(prefix, iri));
            }
        };
        try {
            RDFParser.create()
                    .source(new ByteArrayInputStream(content))
                    .lang(Lang.TURTLE)
                    .base(baseIri)
                    .errorHandler(new FailOnError())
                    .parse(sink);
        } catch (RiotParseException e) {
            throw new SyntaxError(e.getOriginalMessage(), e.getLine(), e.getCol());
        } catch (RiotException e) {
            throw new SyntaxError(e.getMessage(), 1, 1);
        } catch (StackOverflowError e) {
            throw new SyntaxError("Nesting is too deep", 1, 1);
        }
        return new Parsed(graph, new ArrayList<>(prefixes));
    }

    /** Turns parser errors into exceptions and ignores warnings (they do not make the content invalid). */
    private static final class FailOnError implements ErrorHandler {

        @Override
        public void warning(String message, long line, long col) {
            // Warnings such as unusual IRIs do not make the Turtle invalid.
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
