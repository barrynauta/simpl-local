package eu.simpl.vocabularymanager.store;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.Api;
import eu.simpl.vocabularymanager.support.Api.Part;
import eu.simpl.vocabularymanager.support.IntegrationTestBase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.lib.ShLib;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.system.Txn;
import org.junit.jupiter.api.Test;

/** D87: the physical model (docs/physical-model.shacl.ttl) holds for the data the service really stores. */
class PhysicalModelShaclTest extends IntegrationTestBase {

    @Test
    void storedDataConformsToTheShapes() {
        String id = Api.unique();
        String name = "Shacl" + id;
        create(name, simpleTurtle(id));
        newVersion(name, "Second", simpleTurtle(id + "b"));
        api.json("PATCH", "/v1/vocabularies/" + name, ENV.admin(), "{\"status\":\"DEPRECATED\"}");
        api.multipart("/v1/external-vocabularies", ENV.admin(), Part.field("name", "ShaclExt" + id),
                Part.field("namespaceUri", "https://shacl.example/" + id + "#"),
                Part.field("aliasUris", "https://shacl-alias.example/" + id),
                Part.file(simpleTurtle(id)));

        Path file = Path.of("..", "docs", "physical-model.shacl.ttl");
        assertThat(Files.exists(file)).as("shapes file %s", file.toAbsolutePath()).isTrue();
        Shapes shapes = Shapes.parse(RDFDataMgr.loadGraph(file.toString()));
        DatasetGraph dataset = ENV.dataset();
        List<String> failures = new ArrayList<>();
        int[] graphs = {0};
        Txn.executeRead(dataset, () -> dataset.listGraphNodes().forEachRemaining((Node graphName) -> {
            Graph graph = dataset.getGraph(graphName);
            graphs[0]++;
            ValidationReport report = ShaclValidator.get().validate(shapes, graph);
            if (!report.conforms()) {
                ShLib.printReport(report);
                failures.add(graphName.getURI());
            }
        }));
        assertThat(graphs[0]).isGreaterThanOrEqualTo(4);
        assertThat(failures).isEmpty();
    }
}
