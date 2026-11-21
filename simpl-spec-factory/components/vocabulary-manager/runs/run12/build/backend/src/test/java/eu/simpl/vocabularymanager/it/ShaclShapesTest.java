package eu.simpl.vocabularymanager.it;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.store.Rdf;
import java.nio.file.Path;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.lib.ShLib;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.system.Txn;
import org.junit.jupiter.api.Test;

/** D87: stored data conforms to the SHACL shapes of the physical model, graph by graph. */
class ShaclShapesTest extends ApiTestBase {

    @Test
    void storedDataConforms() throws Exception {
        String name = unique("Shacl");
        createVocabulary(name, ttl("ex:a a skos:Concept .\n"));
        createVersion(name, "Second", ttl("ex:b a skos:Concept .\n"));
        json("PATCH", "/v1/vocabularies/" + name, admin, "{\"status\":\"DEPRECATED\"}");
        multipart(
                "/v1/external-vocabularies",
                admin,
                fields(
                        "name",
                        unique("ShaclExt"),
                        "namespaceUri",
                        "http://shacl.example/" + name + "#",
                        "aliasUris",
                        "http://alias.example/" + name,
                        "description",
                        "Copy"),
                "@prefix d: <http://d.example/> . d:a d:b d:c .".getBytes());

        Shapes shapes = Shapes.parse(RDFDataMgr.loadGraph(
                Path.of("../docs/physical-model-shapes.ttl").toString()));
        for (String g : new String[] {Rdf.G_INTERNAL, Rdf.G_CONTENT, Rdf.G_EXTERNAL, Rdf.G_EXTERNAL_CONTENT}) {
            Graph copy = GraphFactory.createDefaultGraph();
            Txn.executeRead(DATASET, () -> DATASET.asDatasetGraph()
                    .getGraph(NodeFactory.createURI(g))
                    .find()
                    .forEachRemaining(copy::add));
            assertThat(copy.size()).as(g).isPositive();
            ValidationReport report = ShaclValidator.get().validate(shapes, copy);
            if (!report.conforms()) {
                ShLib.printReport(report);
            }
            assertThat(report.conforms()).as(g).isTrue();
        }
    }
}
