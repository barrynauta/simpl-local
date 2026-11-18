package eu.simpl.vocabularymanager.validation;

import java.util.List;
import org.apache.jena.graph.Graph;

/**
 * Result of the syntax step: the triples and the prefix declarations in order of appearance.
 *
 * @param graph parsed triples
 * @param prefixes prefix declarations, without exact repeats
 */
public record ParsedContent(Graph graph, List<PrefixDeclaration> prefixes) {}
