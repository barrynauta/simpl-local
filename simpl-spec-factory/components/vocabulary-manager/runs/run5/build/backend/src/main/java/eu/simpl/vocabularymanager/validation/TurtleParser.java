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

/** Syntax validation (D9): the content must parse as Turtle (RDF 1.1). Reports the first parse error only. */
@Component
public class TurtleParser {

    static final String SYNTAX_DETAIL = "Encountered invalid Turtle syntax";

    /**
     * Parses Turtle content.
     *
     * @param content the uploaded bytes
     * @param baseIri base for relative IRIs (D57)
     * @return the triples and prefix declarations
     * @throws ContentValidationException with one error when the content is not valid Turtle
     */
    public ParsedContent parse(byte[] content, String baseIri) {
        Graph graph = GraphFactory.createDefaultGraph();
        Set<PrefixDeclaration> prefixes = new LinkedHashSet<>();
        StreamRDF sink = new StreamRDFWrapper(StreamRDFLib.graph(graph)) {
            @Override
            public void prefix(String prefix, String iri) {
                prefixes.add(new PrefixDeclaration(prefix, iri));
                super.prefix(prefix, iri);
            }
        };
        try {
            RDFParser.create()
                    .source(new ByteArrayInputStream(content))
                    .lang(Lang.TURTLE)
                    .base(baseIri)
                    .errorHandler(new FailOnError())
                    .parse(sink);
        } catch (SyntaxFailure e) {
            throw syntaxError(e.getMessage(), e.line, e.column);
        } catch (RiotParseException e) {
            throw syntaxError(e.getOriginalMessage(), e.getLine(), e.getCol());
        } catch (RiotException e) {
            throw syntaxError(e.getMessage(), -1, -1);
        }
        return new ParsedContent(graph, new ArrayList<>(prefixes));
    }

    private static ContentValidationException syntaxError(String message, long line, long column) {
        String detail = SYNTAX_DETAIL + (message == null || message.isBlank() ? "." : ": " + message);
        String location = line > 0 ? "line " + line + ", column " + Math.max(column, 1) : "document";
        return new ContentValidationException(List.of(new ValidationError(detail, location)));
    }

    /** Stops parsing at the first error; warnings (for example unusual IRIs) do not block. */
    private static final class FailOnError implements ErrorHandler {
        @Override
        public void warning(String message, long line, long col) {
            // Warnings of the parser are not validation errors (D9 names only parse errors).
        }

        @Override
        public void error(String message, long line, long col) {
            throw new SyntaxFailure(message, line, col);
        }

        @Override
        public void fatal(String message, long line, long col) {
            throw new SyntaxFailure(message, line, col);
        }
    }

    /** Internal carrier of the first parse error. */
    private static final class SyntaxFailure extends RuntimeException {
        private final long line;
        private final long column;

        SyntaxFailure(String message, long line, long column) {
            super(message);
            this.line = line;
            this.column = column;
        }
    }
}
