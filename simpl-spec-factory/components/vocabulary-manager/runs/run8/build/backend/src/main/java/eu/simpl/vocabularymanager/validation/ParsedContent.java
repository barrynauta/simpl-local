package eu.simpl.vocabularymanager.validation;

import java.util.List;
import org.apache.jena.graph.Triple;

/**
 * Parsed Turtle content.
 *
 * @param triples the triples in file order
 * @param prefixes the prefix declarations in file order (the vocabulary references of D12)
 */
public record ParsedContent(List<Triple> triples, List<PrefixDeclaration> prefixes) {}
