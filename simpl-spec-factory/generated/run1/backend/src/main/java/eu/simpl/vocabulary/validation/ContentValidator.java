package eu.simpl.vocabulary.validation;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.sparql.graph.GraphFactory;

/**
 * Runs the validation pipeline on an uploaded file: syntax first (SIMPL-18181: when syntax fails, semantic
 * validation is not performed), then structure (D16) and semantics (D17) together, so one response lists all
 * errors (SIMPL-18180 AC7, SIMPL-18183 note).
 */
public class ContentValidator {

    private final String referenceBase;
    private final TurtleSyntaxValidator syntax = new TurtleSyntaxValidator();
    private final StructureValidator structure = new StructureValidator();
    private final SemanticValidator semantic;

    public ContentValidator(String referenceBase) {
        this.referenceBase = referenceBase;
        this.semantic = new SemanticValidator(referenceBase);
    }

    /** Full validation for an internal vocabulary version. */
    public ValidationReport validate(String vocabularyName, int version, byte[] content, ReferenceResolver resolver) {
        String text;
        try {
            text = decode(content);
        } catch (CharacterCodingException e) {
            return new ValidationReport(List.of(new ValidationIssue(ValidationIssue.Category.SYNTAX,
                    "The file is not valid UTF-8 text.", "document")), List.of(), GraphFactory.createDefaultGraph(),
                    null);
        }
        TurtleSyntaxValidator.Outcome parsed = syntax.parse(text, referenceBase + "/" + vocabularyName + "/" + version);
        if (!parsed.errors().isEmpty()) {
            return new ValidationReport(parsed.errors(), List.of(), parsed.graph(), text);
        }
        List<ValidationIssue> errors = new ArrayList<>(structure.validate(parsed.graph()));
        SemanticValidator.Result sem = semantic.validate(vocabularyName, parsed.graph(), resolver);
        errors.addAll(sem.errors());
        return new ValidationReport(errors, sem.warnings(), parsed.graph(), text);
    }

    /** Syntax-only validation, used for cached external vocabularies. */
    public ValidationReport validateSyntaxOnly(String name, byte[] content) {
        String text;
        try {
            text = decode(content);
        } catch (CharacterCodingException e) {
            return new ValidationReport(List.of(new ValidationIssue(ValidationIssue.Category.SYNTAX,
                    "The file is not valid UTF-8 text.", "document")), List.of(), GraphFactory.createDefaultGraph(),
                    null);
        }
        TurtleSyntaxValidator.Outcome parsed = syntax.parse(text, "urn:simpl:external:" + name + "#");
        List<ValidationIssue> errors = new ArrayList<>(parsed.errors());
        if (errors.isEmpty() && parsed.graph().isEmpty()) {
            errors.add(new ValidationIssue(ValidationIssue.Category.STRUCTURE,
                    "The vocabulary file contains no RDF statements.", "document"));
        }
        return new ValidationReport(errors, List.of(), parsed.graph(), text);
    }

    static String decode(byte[] content) throws CharacterCodingException {
        String text = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(content)).toString();
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        return text.replace("\r\n", "\n");
    }
}
