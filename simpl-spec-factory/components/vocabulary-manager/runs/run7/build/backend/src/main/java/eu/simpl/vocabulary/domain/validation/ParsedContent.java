package eu.simpl.vocabulary.domain.validation;

import java.util.List;
import org.apache.jena.graph.Graph;

/**
 * Content that passed syntax validation.
 *
 * @param graph the triples
 * @param prefixes prefix declarations in file order
 */
public record ParsedContent(Graph graph, List<PrefixDeclaration> prefixes) {}
