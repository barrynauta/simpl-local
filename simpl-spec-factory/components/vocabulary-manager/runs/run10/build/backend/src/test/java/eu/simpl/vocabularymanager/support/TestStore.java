package eu.simpl.vocabularymanager.support;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.jena.atlas.web.AuthScheme;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.sparql.core.DatasetGraph;

/** An in-process Fuseki with the same services and users as the deployed store, on an in-memory TDB2 dataset. */
public final class TestStore implements AutoCloseable {

    public static final String READ_USER = "reader";
    public static final String READ_PASSWORD = "test-reader";
    public static final String UPDATE_USER = "writer";
    public static final String UPDATE_PASSWORD = "test-writer";

    private final FusekiServer server;

    public TestStore() {
        server = FusekiServer.create()
                .port(0)
                .loopback(true)
                .passwordFile(resource("fuseki-test-passwd").toString())
                .auth(AuthScheme.BASIC)
                .parseConfigFile(resource("fuseki-test-config.ttl"))
                .build()
                .start();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getHttpPort() + "/vocabulary";
    }

    public DatasetGraph dataset() {
        return server.getDataAccessPointRegistry()
                .get("/vocabulary")
                .getDataService()
                .getDataset();
    }

    private static Path resource(String name) {
        try {
            return Paths.get(TestStore.class.getClassLoader().getResource(name).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        server.stop();
    }
}
