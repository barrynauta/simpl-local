package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.domain.ContentValidationException;
import eu.simpl.vocabularymanager.domain.ValidationIssue;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.sparql.graph.GraphFactory;
import org.springframework.stereotype.Component;

/**
 * Syntax validation (D9 first step): parses Turtle with Apache Jena RIOT and reports the first error with its
 * line and, when the parser gives one, its column (D60, D65).
 */
@Component
public class TurtleParser {

    private static final String SYNTAX_DETAIL = "Encountered invalid Turtle syntax";
    private static final Pattern POSITION = Pattern.compile("\\[line: (\\d+), col: (\\d+)\\s*]");
    private static final int MAX_MESSAGE = 300;

    /**
     * Parses uploaded Turtle content.
     *
     * @param content the uploaded bytes
     * @param baseIri base for relative IRIs (D57)
     * @return the parsed content
     * @throws ContentValidationException with one issue when the content is not valid Turtle
     */
    public ParsedContent parse(byte[] content, String baseIri) {
        Collector collector = new Collector();
        FirstError firstError = new FirstError();
        try {
            RDFParser.create()
                    .source(new ByteArrayInputStream(content))
                    .lang(Lang.TURTLE)
                    .base(baseIri)
                    .errorHandler(firstError)
                    .parse(collector);
        } catch (RiotException ex) {
            firstError.recordIfEmpty(ex.getMessage());
        }
        if (firstError.message != null) {
            throw new ContentValidationException(List.of(firstError.toIssue()));
        }
        return new ParsedContent(collector.graph, List.copyOf(collector.prefixes), collector.subjects);
    }

    /** Collects triples, prefix declarations and subject IRIs. */
    private static final class Collector extends StreamRDFBase {
        private final Graph graph = GraphFactory.createDefaultGraph();
        private final List<PrefixDeclaration> prefixes = new ArrayList<>();
        private final Set<String> subjects = new LinkedHashSet<>();

        @Override
        public void triple(Triple triple) {
            graph.add(triple);
            if (triple.getSubject().isURI()) {
                subjects.add(triple.getSubject().getURI());
            }
        }

        @Override
        public void prefix(String prefix, String iri) {
            PrefixDeclaration declaration = new PrefixDeclaration(prefix, iri);
            if (!prefixes.contains(declaration)) {
                prefixes.add(declaration);
            }
        }
    }

    /** Keeps the first error or fatal error; warnings are not errors. */
    private static final class FirstError implements ErrorHandler {
        private String message;
        private long line = -1;
        private long column = -1;

        @Override
        public void warning(String message, long line, long col) {
            // Warnings never block (D10); the parser goes on.
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

        private void record(String text, long atLine, long atColumn) {
            if (this.message == null) {
                this.message = text == null ? "" : text;
                this.line = atLine;
                this.column = atColumn;
            }
        }

        void recordIfEmpty(String text) {
            if (message != null) {
                return;
            }
            message = text == null ? "" : text;
            Matcher matcher = POSITION.matcher(message);
            if (matcher.find()) {
                line = Long.parseLong(matcher.group(1));
                column = Long.parseLong(matcher.group(2));
            }
        }

        ValidationIssue toIssue() {
            String cleaned = POSITION.matcher(message).replaceAll("").trim();
            if (cleaned.length() > MAX_MESSAGE) {
                cleaned = cleaned.substring(0, MAX_MESSAGE);
            }
            String detail = cleaned.isEmpty() ? SYNTAX_DETAIL + "." : SYNTAX_DETAIL + ": " + cleaned;
            return new ValidationIssue(detail, location());
        }

        private String location() {
            long reportedLine = line > 0 ? line : 1;
            if (column > 0) {
                return "line " + reportedLine + ", column " + column;
            }
            return "line " + reportedLine;
        }
    }
}
