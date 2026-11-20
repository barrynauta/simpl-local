package eu.simpl.vocabularymanager.validation;

import java.util.List;
import java.util.Set;
import org.apache.jena.graph.Graph;

/**
 * Result of a successful Turtle parse.
 *
 * @param graph the triples
 * @param prefixes the prefix declarations in document order
 * @param subjectIris the IRIs used as subject, for the own-namespace rule of D12
 */
public record ParsedContent(Graph graph, List<PrefixDeclaration> prefixes, Set<String> subjectIris) {}
