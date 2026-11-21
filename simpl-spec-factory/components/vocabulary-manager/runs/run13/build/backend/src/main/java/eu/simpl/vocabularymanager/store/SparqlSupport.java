package eu.simpl.vocabularymanager.store;

import java.time.Instant;
import java.util.List;
import org.apache.jena.graph.Node;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.modify.request.UpdateDeleteWhere;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.sparql.syntax.Element;

/**
 * Helpers that build SPARQL with Jena (D94): queries through ParameterizedSparqlString with typed values,
 * updates as Jena update objects whose templates are lists of quads. Values are never joined into text.
 */
final class SparqlSupport {

    static final String PREFIXES = "PREFIX vm: <" + StoreModel.NS + ">\n"
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n";

    private SparqlSupport() {}

    /** A query template with the common prefixes and the named graphs bound. */
    static ParameterizedSparqlString query(String text) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString(PREFIXES + text);
        pss.setParam("gInternal", StoreModel.GRAPH_INTERNAL);
        pss.setParam("gExternal", StoreModel.GRAPH_EXTERNAL);
        pss.setParam("gContent", StoreModel.GRAPH_CONTENT);
        return pss;
    }

    /** The WHERE pattern of a SELECT template, for use in an update. */
    static Element where(ParameterizedSparqlString pss) {
        Query parsed = pss.asQuery();
        return parsed.getQueryPattern();
    }

    /** DELETE { deletes } INSERT { inserts } WHERE { where }. */
    static UpdateModify modify(List<Quad> deletes, List<Quad> inserts, Element where) {
        UpdateModify modify = new UpdateModify();
        deletes.forEach(modify.getDeleteAcc()::addQuad);
        inserts.forEach(modify.getInsertAcc()::addQuad);
        modify.setHasDeleteClause(!deletes.isEmpty());
        modify.setHasInsertClause(!inserts.isEmpty());
        modify.setElement(where);
        return modify;
    }

    static UpdateDeleteWhere deleteWhere(List<Quad> pattern) {
        org.apache.jena.sparql.modify.request.QuadAcc acc = new org.apache.jena.sparql.modify.request.QuadAcc();
        pattern.forEach(acc::addQuad);
        return new UpdateDeleteWhere(acc);
    }

    static Quad quad(Node graph, Node subject, Node predicate, Node object) {
        return new Quad(graph, subject, predicate, object);
    }

    static String rowText(QuerySolution row, String variable) {
        RDFNode node = row.get(variable);
        return node == null ? null : node.asLiteral().getLexicalForm();
    }

    static int rowInt(QuerySolution row, String variable) {
        return Integer.parseInt(row.getLiteral(variable).getLexicalForm());
    }

    static long longValue(QuerySolution row, String variable) {
        return Long.parseLong(row.getLiteral(variable).getLexicalForm());
    }

    static Instant instant(QuerySolution row, String variable) {
        return Instant.parse(row.getLiteral(variable).getLexicalForm());
    }
}
