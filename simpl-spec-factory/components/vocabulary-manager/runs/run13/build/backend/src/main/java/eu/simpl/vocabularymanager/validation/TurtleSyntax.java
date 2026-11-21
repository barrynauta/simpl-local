package eu.simpl.vocabularymanager.validation;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemIssue;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.store.ContentCodec;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.StreamRDFBase;

/**
 * The syntax step of D9: the content must parse as Turtle (RDF 1.1) with the Jena RIOT parser. The first
 * error gives one issue with its line and column (column only when the parser gives one, D60, D65).
 */
public final class TurtleSyntax {

    public static final String SYNTAX_DETAIL = "Encountered invalid Turtle syntax";
    private static final char BYTE_ORDER_MARK = '﻿';

    private TurtleSyntax() {}

    /** Thrown by the error handler on the first parse error. */
    private static final class FirstError extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final long line;
        private final long column;

        FirstError(String message, long line, long column) {
            super(message, null, false, false);
            this.line = line;
            this.column = column;
        }
    }

    /**
     * Parses the bytes.
     *
     * @param baseIri base for relative IRIs (D57, D124)
     * @throws ApiException 400 with the syntax issue
     */
    public static ParsedContent parse(byte[] bytes, String baseIri) {
        String text = ContentCodec.strictUtf8(bytes);
        if (text == null) {
            throw syntaxError("the file is not valid UTF-8", firstInvalidLine(bytes), -1);
        }
        if (!text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK) {
            text = text.substring(1);
        }
        Collector collector = new Collector();
        try {
            RDFParser.create()
                    .fromString(text)
                    .lang(Lang.TURTLE)
                    .base(baseIri)
                    .errorHandler(new FailFast())
                    .parse(collector);
        } catch (FirstError e) {
            throw syntaxError(e.getMessage(), e.line, e.column);
        } catch (RiotException e) {
            throw syntaxError(e.getMessage(), -1, -1);
        }
        return new ParsedContent(collector.triples, collector.prefixes);
    }

    private static ApiException syntaxError(String message, long line, long column) {
        String location = line < 1 ? "document" : column < 1 ? "line " + line : "line " + line + ", column " + column;
        String detail = SYNTAX_DETAIL + (message == null || message.isBlank() ? "." : ": " + clean(message));
        return Problems.badRequest(ProblemIssue.invalidInput("body", "vocabularyFile", detail, location));
    }

    private static String clean(String message) {
        String single = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        return single.length() > 300 ? single.substring(0, 300) + "..." : single;
    }

    /** Line of the first byte that is not valid UTF-8. */
    private static long firstInvalidLine(byte[] bytes) {
        java.nio.charset.CharsetDecoder decoder = java.nio.charset.StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT);
        java.nio.ByteBuffer in = java.nio.ByteBuffer.wrap(bytes);
        java.nio.CharBuffer out = java.nio.CharBuffer.allocate(bytes.length + 1);
        decoder.decode(in, out, true);
        int offset = in.position();
        long line = 1;
        for (int i = 0; i < offset && i < bytes.length; i++) {
            if (bytes[i] == '\n') {
                line++;
            }
        }
        return line;
    }

    /** Stops at the first error; warnings do not block. */
    private static final class FailFast implements ErrorHandler {
        @Override
        public void warning(String message, long line, long col) {
            // Warnings (for example unusual IRIs) never block (D10).
        }

        @Override
        public void error(String message, long line, long col) {
            throw new FirstError(message, line, col);
        }

        @Override
        public void fatal(String message, long line, long col) {
            throw new FirstError(message, line, col);
        }
    }

    /** Collects triples and prefixes in file order. */
    private static final class Collector extends StreamRDFBase {
        private final java.util.Map<Triple, Integer> triples = new java.util.LinkedHashMap<>();
        private final List<ParsedContent.PrefixDeclaration> prefixes = new ArrayList<>();
        private int position;

        @Override
        public void triple(Triple triple) {
            triples.putIfAbsent(triple, position++);
        }

        @Override
        public void prefix(String prefix, String iri) {
            prefixes.add(new ParsedContent.PrefixDeclaration(prefix, iri, position++));
        }
    }
}
