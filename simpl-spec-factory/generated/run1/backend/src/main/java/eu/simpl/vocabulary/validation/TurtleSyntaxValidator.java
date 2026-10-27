package eu.simpl.vocabulary.validation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.RiotParseException;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.sparql.graph.GraphFactory;

/**
 * Syntax validation (SIMPL-18180, 18181, D16). Parses Turtle with Apache Jena and reports every error with
 * "line L, column C". Jena stops at the first fatal error, so after a fatal error the validator skips to the end
 * of the broken statement (the next line ending with '.') and parses the rest again, with the prefix
 * declarations seen so far. This way one response lists all syntax errors instead of the first one only.
 */
public class TurtleSyntaxValidator {

    private static final int MAX_ERRORS = 100;
    private static final Pattern DIRECTIVE = Pattern.compile("^\\s*(@prefix|@base|PREFIX|BASE)\\b.*",
            Pattern.CASE_INSENSITIVE);

    /** Outcome of parsing: the graph (only meaningful without errors) and the errors found. */
    public record Outcome(Graph graph, List<ValidationIssue> errors) {}

    private record Err(long line, long col, String message) {}

    public Outcome parse(String text, String base) {
        String[] lines = text.split("\n", -1);
        Set<Err> errors = new LinkedHashSet<>();
        Set<Integer> errorLines = new java.util.HashSet<>();
        Graph firstGraph = null;
        int start = 0;
        while (start < lines.length && errors.size() < MAX_ERRORS) {
            List<String> header = new ArrayList<>();
            for (int i = 0; i < start; i++) {
                if (DIRECTIVE.matcher(lines[i]).matches() && !errorLines.contains(i + 1)) {
                    header.add(lines[i]);
                }
            }
            StringBuilder chunk = new StringBuilder();
            for (String h : header) {
                chunk.append(h).append('\n');
            }
            for (int i = start; i < lines.length; i++) {
                chunk.append(lines[i]);
                if (i < lines.length - 1) {
                    chunk.append('\n');
                }
            }
            int headerLines = header.size();
            int offset = start - headerLines; // absolute line = relative line + offset

            Graph graph = GraphFactory.createDefaultGraph();
            Collector collector = new Collector();
            Err fatal = null;
            try {
                RDFParser.create().fromString(chunk.toString()).lang(Lang.TURTLE).base(base)
                        .errorHandler(collector).parse(StreamRDFLib.graph(graph));
            } catch (RiotParseException e) {
                fatal = new Err(e.getLine(), e.getCol(), e.getOriginalMessage());
            } catch (RiotException e) {
                fatal = collector.lastFatal != null ? collector.lastFatal : new Err(-1, -1, e.getMessage());
            }
            if (firstGraph == null) {
                firstGraph = graph;
            }
            for (Err e : collector.errors) {
                errors.add(shift(e, offset));
                if (e.line() > 0) {
                    errorLines.add((int) (e.line() + offset));
                }
            }
            if (fatal == null) {
                break;
            }
            Err shifted = shift(fatal, offset);
            errors.add(shifted);
            if (shifted.line() <= 0 || shifted.line() <= start) {
                // Error without a position or inside the replayed header: no safe way to continue.
                break;
            }
            errorLines.add((int) shifted.line());
            int next = -1;
            for (int i = (int) shifted.line() - 1; i < lines.length; i++) {
                if (lines[i].trim().endsWith(".")) {
                    next = i + 1;
                    break;
                }
            }
            if (next < 0 || next <= start) {
                break;
            }
            start = next;
        }
        List<ValidationIssue> issues = new ArrayList<>();
        for (Err e : errors) {
            String location = e.line() > 0 ? "line " + e.line() + ", column " + Math.max(e.col(), 1) : "document";
            issues.add(new ValidationIssue(ValidationIssue.Category.SYNTAX,
                    "Invalid Turtle syntax: " + e.message(), location));
        }
        return new Outcome(firstGraph == null ? GraphFactory.createDefaultGraph() : firstGraph, issues);
    }

    private static Err shift(Err e, int offset) {
        return e.line() > 0 ? new Err(e.line() + offset, e.col(), e.message()) : e;
    }

    /** Collects errors instead of throwing, except on fatal errors (the parser cannot continue). */
    private static final class Collector implements ErrorHandler {
        private final List<Err> errors = new ArrayList<>();
        private Err lastFatal;

        @Override
        public void warning(String message, long line, long col) {
            // Warnings (for example unusual IRIs) do not block a vocabulary.
        }

        @Override
        public void error(String message, long line, long col) {
            errors.add(new Err(line, col, message));
        }

        @Override
        public void fatal(String message, long line, long col) {
            lastFatal = new Err(line, col, message);
            throw new RiotParseException(message, line, col);
        }
    }
}
