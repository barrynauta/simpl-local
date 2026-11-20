package eu.simpl.vocabularymanager.store;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.ApiClient;
import eu.simpl.vocabularymanager.support.IntegrationTestBase;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.system.Txn;
import org.junit.jupiter.api.Test;

/** D87: the SHACL shapes of docs/physical-model-shapes.ttl hold for data stored through the API. */
class PhysicalModelShapesTest extends IntegrationTestBase {

    @Test
    void storedDataConformsToTheShapes() {
        String name = uniqueName("Shape");
        assertThat(api().multipart(
                                "/v1/vocabularies", IDP.admin(), ApiClient.createParts(name, "D", simpleTurtle(name)))
                        .status())
                .isEqualTo(201);
        api().multipart(
                        "/v1/vocabularies/" + name + "/versions",
                        IDP.admin(),
                        ApiClient.versionParts("D2", "C2", simpleTurtle(name) + "# 2\n"));
        String external = uniqueName("Shx");
        api().multipart(
                        "/v1/external-vocabularies",
                        IDP.admin(),
                        List.of(
                                ApiClient.Part.text("name", external),
                                ApiClient.Part.text("namespaceUri", "https://shapes.example.org/" + external),
                                ApiClient.Part.text("aliasUris", "https://alias.example.org/" + external),
                                ApiClient.Part.file(
                                        "vocabularyFile", simpleTurtle(external).getBytes())));

        Shapes shapes = Shapes.parse(RDFDataMgr.loadGraph(
                Path.of("../docs/physical-model-shapes.ttl").toString()));
        List<String> graphs = new ArrayList<>();
        Txn.executeRead(STORE.dataset(), () -> {
            List<Node> names = new ArrayList<>();
            STORE.dataset().listGraphNodes().forEachRemaining(names::add);
            for (Node graphName : names) {
                Graph copy = GraphFactory.createDefaultGraph();
                STORE.dataset().getGraph(graphName).find().forEachRemaining(copy::add);
                ValidationReport report = ShaclValidator.get().validate(shapes, copy);
                assertThat(report.conforms())
                        .as(graphName + " " + report.getEntries())
                        .isTrue();
                graphs.add(graphName.getURI());
            }
        });
        assertThat(graphs)
                .contains(
                        Rdf.INTERNAL_GRAPH,
                        Rdf.EXTERNAL_GRAPH,
                        Rdf.contentGraph(name, 1),
                        Rdf.contentGraph(name, 2),
                        Rdf.externalContentGraph(external));
    }
}
