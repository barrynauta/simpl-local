package eu.simpl.vocabularymanager.support;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.tdb2.TDB2Factory;

/** An embedded Fuseki with an in-memory TDB2 dataset, the same store product as the stack (E3). */
public final class TestStore {

    private final FusekiServer server;
    private final Dataset dataset;

    public TestStore() {
        dataset = TDB2Factory.createDataset();
        server = FusekiServer.create().port(0).add("/vocabulary", dataset, true).build();
        server.start();
    }

    public Dataset dataset() {
        return dataset;
    }

    public String queryUrl() {
        return "http://localhost:" + server.getHttpPort() + "/vocabulary/query";
    }

    public String updateUrl() {
        return "http://localhost:" + server.getHttpPort() + "/vocabulary/update";
    }

    public void stop() {
        server.stop();
    }
}
