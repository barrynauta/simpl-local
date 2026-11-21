package eu.simpl.vocabularymanager;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.store.StoreModel;
import eu.simpl.vocabularymanager.support.ApiClient;
import eu.simpl.vocabularymanager.support.ApiClient.Part;
import eu.simpl.vocabularymanager.support.IntegrationTestBase;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.system.Txn;
import org.junit.jupiter.api.Test;

/** D87: the SHACL shapes of the physical model hold for the data the service stores. */
class PhysicalModelShaclTest extends IntegrationTestBase {

    @Test
    void storedDataConformsToTheShapes() {
        String name = uniqueName("Shape");
        api.multipart("/v1/vocabularies", admin, ApiClient.createParts(name, simpleTurtle(name), "Desc."));
        api.multipart(
                "/v1/vocabularies/" + name + "/versions",
                admin,
                ApiClient.versionParts(simpleTurtle(name) + "# 2\n", "Desc 2.", "Two."));
        api.json("PATCH", "/v1/vocabularies/" + name, admin, "{\"status\":\"DEPRECATED\"}");
        String ext = uniqueName("ShapeExt");
        api.multipart(
                "/v1/external-vocabularies",
                admin,
                List.of(
                        Part.file(
                                "vocabularyFile",
                                "<http://s.example/a> <http://s.example/b> \"c\" .".getBytes(StandardCharsets.UTF_8)),
                        Part.field("name", ext),
                        Part.field("namespaceUri", "http://s.example/" + ext + "#"),
                        Part.field("aliasUris", "http://alias.s.example/" + ext),
                        Part.field("description", "Shapes")));

        Shapes shapes = Shapes.parse(RDFDataMgr.loadGraph("../docs/physical-model-shapes.ttl"));
        Txn.executeRead(STORE.dataset(), () -> {
            for (var graphName :
                    List.of(StoreModel.GRAPH_INTERNAL, StoreModel.GRAPH_EXTERNAL, StoreModel.GRAPH_CONTENT)) {
                Graph graph = STORE.dataset().asDatasetGraph().getGraph(graphName);
                assertThat(graph.size()).as(graphName.getURI()).isPositive();
                ValidationReport report = ShaclValidator.get().validate(shapes, graph);
                assertThat(report.conforms())
                        .as(graphName.getURI() + " " + report.getEntries())
                        .isTrue();
            }
        });
    }
}
