package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.error.Issue;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
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

/** Syntax validation: the content must parse as Turtle (RDF 1.1). Only the first error is reported (decision D9). */
@Component
public class TurtleSyntaxValidator {

    public static final String FIELD = "vocabularyFile";
    static final String SYNTAX_DETAIL = "Encountered invalid Turtle syntax";

    /** Parses the content; relative IRIs are resolved against the base IRI. */
    public ParsedTurtle parse(byte[] content, String baseIri) {
        Graph graph = GraphFactory.createDefaultGraph();
        List<PrefixDeclaration> prefixes = new ArrayList<>();
        StreamRDF destination = new StreamRDFWrapper(StreamRDFLib.graph(graph)) {
            @Override
            public void prefix(String prefix, String iri) {
                prefixes.add(new PrefixDeclaration(prefix, iri));
                super.prefix(prefix, iri);
            }
        };
        FirstErrorHandler handler = new FirstErrorHandler();
        try {
            RDFParser.create()
                    .source(new ByteArrayInputStream(content))
                    .lang(Lang.TURTLE)
                    .base(baseIri)
                    .errorHandler(handler)
                    .parse(destination);
        } catch (RiotParseException e) {
            handler.record(e.getOriginalMessage(), e.getLine(), e.getCol());
        } catch (RiotException e) {
            handler.record(e.getMessage(), -1, -1);
        }
        if (handler.message == null) {
            return new ParsedTurtle(graph, List.copyOf(prefixes), null);
        }
        Issue issue = Issue.invalidInput(FIELD, detail(handler.message), location(handler.line, handler.column));
        return new ParsedTurtle(GraphFactory.createDefaultGraph(), List.copyOf(prefixes), issue);
    }

    private static String detail(String parserMessage) {
        if (parserMessage == null || parserMessage.isBlank()) {
            return SYNTAX_DETAIL + ".";
        }
        return SYNTAX_DETAIL + ": " + parserMessage.strip();
    }

    private static String location(long line, long column) {
        return "line " + Math.max(line, 1) + ", column " + Math.max(column, 1);
    }

    /** Keeps the first error and stops parsing; warnings do not block. */
    private static final class FirstErrorHandler implements ErrorHandler {
        private String message;
        private long line = -1;
        private long column = -1;

        void record(String text, long atLine, long atColumn) {
            if (message == null) {
                message = text == null ? "" : text;
                line = atLine;
                column = atColumn;
            }
        }

        @Override
        public void warning(String text, long atLine, long atColumn) {
            // Warnings (for example unusual IRIs) do not make the syntax invalid.
        }

        @Override
        public void error(String text, long atLine, long atColumn) {
            record(text, atLine, atColumn);
            throw new RiotParseException(text, atLine, atColumn);
        }

        @Override
        public void fatal(String text, long atLine, long atColumn) {
            record(text, atLine, atColumn);
            throw new RiotParseException(text, atLine, atColumn);
        }
    }
}
