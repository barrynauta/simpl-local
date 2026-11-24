package eu.simpl.vocabularymanager.validation;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.RiotParseException;
import org.apache.jena.riot.lang.LabelToNode;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.riot.tokens.Token;
import org.apache.jena.riot.tokens.TokenType;
import org.apache.jena.riot.tokens.Tokenizer;
import org.apache.jena.riot.tokens.TokenizerText;

/**
 * Syntax validation (D9): parses the content as Turtle (RDF 1.1) with Jena RIOT. The first error stops the parse.
 * A second pass with the Jena tokenizer reads the prefix namespaces as written and the blank node labels.
 */
public final class TurtleSyntax {

    private TurtleSyntax() {}

    /** A Turtle syntax error with its position; line and column are -1 when the parser gives none. */
    public static final class SyntaxError extends Exception {
        private static final long serialVersionUID = 1L;
        private final long line;
        private final long column;

        public SyntaxError(String message, long line, long column) {
            super(message);
            this.line = line;
            this.column = column;
        }

        public long line() {
            return line;
        }

        public long column() {
            return column;
        }

        /** The location text of D9 and D60: 'line N, column M', the column only when known. */
        public String location() {
            if (line < 1) {
                return "document";
            }
            return column < 1 ? "line " + line : "line " + line + ", column " + column;
        }
    }

    /**
     * Parses the content.
     *
     * @param content the uploaded bytes
     * @param base the base IRI for relative IRIs (D57, D124)
     * @return the parsed content
     * @throws SyntaxError on the first syntax error
     */
    public static ParsedContent parse(byte[] content, String base) throws SyntaxError {
        List<Triple> triples = new ArrayList<>();
        List<Pair<String, String>> prefixEvents = new ArrayList<>();
        List<Integer> prefixPositions = new ArrayList<>();
        StreamRDFBase collector = new StreamRDFBase() {
            @Override
            public void triple(Triple triple) {
                triples.add(triple);
            }

            @Override
            public void prefix(String prefix, String iri) {
                prefixEvents.add(Pair.create(prefix, iri));
                prefixPositions.add(triples.size());
            }
        };
        try {
            RDFParser.create()
                    .source(new ByteArrayInputStream(content))
                    .lang(Lang.TURTLE)
                    .base(base)
                    .labelToNode(LabelToNode.createUseLabelAsGiven())
                    .errorHandler(new FailFast())
                    .parse(collector);
        } catch (RiotParseException e) {
            throw new SyntaxError(e.getOriginalMessage(), e.getLine(), e.getCol());
        } catch (RiotException e) {
            throw new SyntaxError(e.getMessage(), -1, -1);
        }
        Lexical lexical = lexicalPass(content);
        List<ParsedContent.PrefixDeclaration> prefixes = new ArrayList<>();
        for (int i = 0; i < prefixEvents.size(); i++) {
            Pair<String, String> event = prefixEvents.get(i);
            String declared = i < lexical.declared().size() ? lexical.declared().get(i) : event.getRight();
            prefixes.add(new ParsedContent.PrefixDeclaration(
                    event.getLeft(), declared, event.getRight(), prefixPositions.get(i)));
        }
        return new ParsedContent(List.copyOf(triples), List.copyOf(prefixes), lexical.blankNodeLabels());
    }

    private record Lexical(List<String> declared, Set<String> blankNodeLabels) {}

    private static Lexical lexicalPass(byte[] content) {
        List<String> declared = new ArrayList<>();
        Set<String> labels = new HashSet<>();
        Tokenizer tokenizer =
                TokenizerText.create().source(new ByteArrayInputStream(content)).build();
        boolean inPrefix = false;
        while (tokenizer.hasNext()) {
            Token token = tokenizer.next();
            if (isPrefixDirective(token)) {
                inPrefix = true;
            } else if (inPrefix && token.getType() == TokenType.IRI) {
                declared.add(token.getImage());
                inPrefix = false;
            } else if (token.getType() == TokenType.BNODE) {
                labels.add(token.getImage());
            }
        }
        return new Lexical(declared, labels);
    }

    private static boolean isPrefixDirective(Token token) {
        if (token.getType() == TokenType.DIRECTIVE) {
            return "prefix".equals(token.getImage());
        }
        return token.getType() == TokenType.KEYWORD
                && "prefix".equals(token.getImage().toLowerCase(Locale.ROOT));
    }

    /** Ignores warnings, stops at the first error (D9: one issue for the first parse error). */
    private static final class FailFast implements ErrorHandler {
        @Override
        public void warning(String message, long line, long col) {
            // Warnings never block (for example IRI advice); only errors are syntax errors.
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
