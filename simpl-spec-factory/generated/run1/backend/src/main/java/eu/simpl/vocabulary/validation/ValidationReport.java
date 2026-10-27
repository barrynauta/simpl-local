package eu.simpl.vocabulary.validation;

import java.util.List;
import org.apache.jena.graph.Graph;

/** Result of validating one uploaded vocabulary file. */
public record ValidationReport(List<ValidationIssue> errors, List<ValidationWarning> warnings, Graph graph,
                               String text) {

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
