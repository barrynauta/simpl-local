package eu.simpl.vocabularymanager.store;

import java.util.Collection;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;

/** Fills an empty VALUES block of a parsed query with typed nodes, without text building (D94). */
final class ValuesBinder {

    private ValuesBinder() {}

    static void bind(Query query, String variable, Collection<Node> nodes) {
        Var var = Var.alloc(variable);
        ElementWalker.walk(query.getQueryPattern(), new ElementVisitorBase() {
            @Override
            public void visit(ElementData data) {
                if (data.getVars().contains(var) && data.getRows().isEmpty()) {
                    nodes.forEach(node -> data.add(BindingFactory.binding(var, node)));
                }
            }
        });
    }
}
