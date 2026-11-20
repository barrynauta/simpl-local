package eu.simpl.vocabularymanager.it;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.store.StoreSchema;
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

/** D87: the SHACL shapes of docs/physical-model.ttl hold for the data the service stores. */
class StoredDataShapesIT extends IntegrationTest {

    @Test
    void storedGraphsConformToThePhysicalModel() {
        String name = uniqueName("Shape");
        create(name, ttl(name));
        newVersion(name, "Two", ttl(name) + "#2");
        api.patch("/v1/vocabularies/" + name + "/versions/1", TestEnvironment.admin(), "{\"status\":\"ACTIVE\"}");
        api.post(
                "/v1/external-vocabularies",
                TestEnvironment.admin(),
                new ApiClient.Form()
                        .field("name", uniqueName("ShapeExt"))
                        .field("namespaceUri", "https://shape.example/" + System.nanoTime() + "#")
                        .field("aliasUris", "https://shape-alias.example/" + System.nanoTime())
                        .field("description", "d")
                        .file("vocabularyFile", "<a> <b> <c> ."));
        Shapes shapes = Shapes.parse(RDFDataMgr.loadGraph("../docs/physical-model.ttl"));
        for (String graph : new String[] {
            StoreSchema.GRAPH_INTERNAL,
            StoreSchema.GRAPH_INTERNAL_CONTENT,
            StoreSchema.GRAPH_EXTERNAL,
            StoreSchema.GRAPH_EXTERNAL_CONTENT
        }) {
            Graph copy = GraphFactory.createDefaultGraph();
            Txn.executeRead(
                    TestEnvironment.DATASET,
                    () -> TestEnvironment.DATASET
                            .getGraph(NodeFactory.createURI(graph))
                            .find()
                            .forEachRemaining(copy::add));
            assertThat(copy.size()).as(graph).isPositive();
            ValidationReport report = ShaclValidator.get().validate(shapes, copy);
            if (!report.conforms()) {
                ShLib.printReport(report);
            }
            assertThat(report.conforms()).as(graph).isTrue();
        }
    }
}
