package eu.simpl.vocabularymanager.support;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;

/** An in-process Fuseki with an in-memory transactional dataset, shared by the tests of one JVM. */
public final class TestFuseki {

    private static FusekiServer server;
    private static DatasetGraph dataset;

    private TestFuseki() {}

    /** Starts the server once. */
    public static synchronized FusekiServer server() {
        if (server == null) {
            dataset = DatasetGraphFactory.createTxnMem();
            server = FusekiServer.create()
                    .port(0)
                    .add("/vocabulary", dataset, true)
                    .build();
            server.start();
        }
        return server;
    }

    /** @return the dataset behind the server */
    public static DatasetGraph dataset() {
        server();
        return dataset;
    }

    /** @return the query endpoint */
    public static String queryUrl() {
        return "http://localhost:" + server().getHttpPort() + "/vocabulary/query";
    }

    /** @return the update endpoint */
    public static String updateUrl() {
        return "http://localhost:" + server().getHttpPort() + "/vocabulary/update";
    }
}
