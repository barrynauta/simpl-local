package eu.simpl.vocabulary.validation;

import java.util.ArrayList;
import java.util.List;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;

/**
 * D16 structure rules: the graph is not empty, and it declares at least one owl:Ontology or skos:ConceptScheme.
 */
public class StructureValidator {

    private static final Node ONTOLOGY = OWL.Ontology.asNode();
    private static final Node CONCEPT_SCHEME = NodeFactory.createURI(SKOS.ConceptScheme.getURI());

    public List<ValidationIssue> validate(Graph graph) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (graph.isEmpty()) {
            issues.add(new ValidationIssue(ValidationIssue.Category.STRUCTURE,
                    "The vocabulary file contains no RDF statements.", "document"));
        }
        boolean declared = graph.contains(Node.ANY, RDF.type.asNode(), ONTOLOGY)
                || graph.contains(Node.ANY, RDF.type.asNode(), CONCEPT_SCHEME);
        if (!declared) {
            issues.add(new ValidationIssue(ValidationIssue.Category.STRUCTURE,
                    "The vocabulary must declare at least one owl:Ontology or skos:ConceptScheme.", "document"));
        }
        return issues;
    }
}
