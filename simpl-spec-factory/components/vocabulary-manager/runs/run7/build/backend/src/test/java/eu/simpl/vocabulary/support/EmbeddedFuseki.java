package eu.simpl.vocabulary.support;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.sparql.core.DatasetGraphFactory;

/** An in-process Fuseki with an in-memory transactional dataset 'vocabulary' (store in a test container). */
public final class EmbeddedFuseki {

    private static FusekiServer server;

    private EmbeddedFuseki() {}

    public static synchronized String url() {
        if (server == null) {
            server = FusekiServer.create()
                    .port(0)
                    .loopback(true)
                    .add("/vocabulary", DatasetGraphFactory.createTxnMem(), true)
                    .build()
                    .start();
        }
        return "http://localhost:" + server.getPort() + "/vocabulary";
    }
}
