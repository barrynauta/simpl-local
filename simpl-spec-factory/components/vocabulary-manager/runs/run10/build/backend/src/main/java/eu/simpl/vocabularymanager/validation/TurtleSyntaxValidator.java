package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.Problems;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.riot.system.StreamRDFWrapper;
import org.apache.jena.sparql.graph.GraphFactory;

/**
 * Syntax validation (D9): the content must parse as Turtle (RDF 1.1) with the Jena RIOT parser. On failure one issue
 * for the first parse error is returned, with its line and, when the parser gives one, its column (D60, D65).
 */
public class TurtleSyntaxValidator {

    static final String SYNTAX_DETAIL = "Encountered invalid Turtle syntax";

    /**
     * Parses the content against the given base IRI (D57).
     *
     * @throws eu.simpl.vocabularymanager.error.ProblemException 400 with one syntax issue
     */
    public ParsedContent parse(byte[] content, String baseIri) {
        Graph graph = GraphFactory.createDefaultGraph();
        Set<ParsedContent.PrefixDeclaration> prefixes = new LinkedHashSet<>();
        StreamRDF destination = new StreamRDFWrapper(StreamRDFLib.graph(graph)) {
            @Override
            public void prefix(String prefix, String iri) {
                prefixes.add(new ParsedContent.PrefixDeclaration(prefix, iri));
                super.prefix(prefix, iri);
            }
        };
        FirstErrorHandler errors = new FirstErrorHandler();
        try {
            RDFParser.create()
                    .source(new ByteArrayInputStream(content))
                    .lang(Lang.TURTLE)
                    .base(baseIri)
                    .errorHandler(errors)
                    .parse(destination);
        } catch (RiotException | IllegalArgumentException e) {
            errors.recordIfFirst(e.getMessage(), -1, -1);
        }
        if (errors.message != null) {
            throw Problems.badRequest(syntaxIssue(errors.message, errors.line, errors.column));
        }
        return new ParsedContent(graph, new ArrayList<>(prefixes));
    }

    static Issue syntaxIssue(String message, long line, long column) {
        String location;
        if (line > 0 && column > 0) {
            location = "line " + line + ", column " + column;
        } else if (line > 0) {
            location = "line " + line;
        } else {
            location = "document";
        }
        String reason = message == null ? "" : message.replaceAll("\\s+", " ").trim();
        if (reason.length() > 300) {
            reason = reason.substring(0, 300);
        }
        String detail = reason.isEmpty() ? SYNTAX_DETAIL + "." : SYNTAX_DETAIL + ": " + reason;
        return Issue.invalidInput("vocabularyFile", detail, location);
    }

    /** Keeps the first error with its position and stops the parse. Warnings are ignored. */
    static final class FirstErrorHandler implements ErrorHandler {
        private String message;
        private long line = -1;
        private long column = -1;

        @Override
        public void warning(String message, long line, long col) {
            // Warnings never block (D10); the parser goes on.
        }

        @Override
        public void error(String message, long line, long col) {
            recordIfFirst(message, line, col);
            throw new RiotException(message);
        }

        @Override
        public void fatal(String message, long line, long col) {
            recordIfFirst(message, line, col);
            throw new RiotException(message);
        }

        void recordIfFirst(String text, long atLine, long atColumn) {
            if (message == null) {
                message = text == null ? "" : text;
                line = atLine;
                column = atColumn;
            }
        }
    }
}
