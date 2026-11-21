package eu.simpl.vocabularymanager.validation;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.StreamRDFBase;

/**
 * The result of the syntax step (D9): the triples in file order and the namespace declarations
 * (@prefix and PREFIX) in file order. Each triple and each declaration has a position (one shared
 * counter) so issues of different rules can be put in file order (D125).
 *
 * @param triples distinct triples in file order
 * @param triplePositions the position of each triple
 * @param prefixes the namespace declarations in file order
 */
public record ParsedTurtle(List<Triple> triples, List<Integer> triplePositions, List<Prefix> prefixes) {

    /**
     * One namespace declaration.
     *
     * @param prefix the prefix label ('' for the empty prefix)
     * @param namespace the namespace IRI
     * @param position position in the file
     */
    public record Prefix(String prefix, String namespace, int position) {}

    /** The first syntax error, with the line and, when the parser gives one, the column. */
    public static final class SyntaxError extends Exception {
        private static final long serialVersionUID = 1L;
        private final long line;
        private final long column;

        public SyntaxError(String message, long line, long column) {
            super(message);
            this.line = line;
            this.column = column;
        }

        /** Location text of D9 and D65: 'line N, column M', column only when known. */
        public String location() {
            if (line <= 0) {
                return "document";
            }
            return column > 0 ? "line " + line + ", column " + column : "line " + line;
        }
    }

    /** Parses Turtle (RDF 1.1) with Jena RIOT; relative IRIs resolve against {@code base} (D57). */
    public static ParsedTurtle parse(byte[] bytes, String base) throws SyntaxError {
        Collector collector = new Collector();
        FirstError handler = new FirstError();
        try {
            RDFParser.create()
                    .source(new ByteArrayInputStream(bytes))
                    .lang(Lang.TURTLE)
                    .base(base)
                    .errorHandler(handler)
                    .parse(collector);
        } catch (RiotException e) {
            if (handler.message != null) {
                throw new SyntaxError(handler.message, handler.line, handler.column);
            }
            throw new SyntaxError(e.getMessage() == null ? "parse error" : e.getMessage(), -1, -1);
        } catch (RuntimeException e) {
            String msg = handler.message != null ? handler.message : String.valueOf(e.getMessage());
            throw new SyntaxError(msg, handler.line, handler.column);
        }
        if (handler.message != null) {
            throw new SyntaxError(handler.message, handler.line, handler.column);
        }
        return new ParsedTurtle(
                new ArrayList<>(collector.triples.keySet()),
                new ArrayList<>(collector.triples.values()),
                collector.prefixes);
    }

    private static final class Collector extends StreamRDFBase {
        private final Map<Triple, Integer> triples = new LinkedHashMap<>();
        private final List<Prefix> prefixes = new ArrayList<>();
        private int position;

        @Override
        public void triple(Triple triple) {
            position++;
            triples.putIfAbsent(triple, position);
        }

        @Override
        public void prefix(String prefix, String iri) {
            position++;
            prefixes.add(new Prefix(prefix, iri, position));
        }
    }

    private static final class FirstError implements ErrorHandler {
        private String message;
        private long line = -1;
        private long column = -1;

        @Override
        public void warning(String message, long line, long col) {
            // Warnings never block (D10).
        }

        @Override
        public void error(String message, long line, long col) {
            record(message, line, col);
            throw new RiotException(message);
        }

        @Override
        public void fatal(String message, long line, long col) {
            record(message, line, col);
            throw new RiotException(message);
        }

        private void record(String message, long line, long col) {
            if (this.message == null) {
                this.message = message;
                this.line = line;
                this.column = col;
            }
        }
    }
}
