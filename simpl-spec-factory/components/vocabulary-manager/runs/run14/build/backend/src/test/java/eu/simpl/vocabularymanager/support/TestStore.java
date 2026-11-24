package eu.simpl.vocabularymanager.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.jena.atlas.web.AuthScheme;
import org.apache.jena.fuseki.main.FusekiServer;

/**
 * An in-process Fuseki with the real store configuration of the stack (fuseki/config.ttl): three users, no
 * anonymous access, TDB2 in memory.
 */
public final class TestStore {

    public static final String READ_USER = "vm_read";
    public static final String READ_PASSWORD = "read-secret";
    public static final String UPDATE_USER = "vm_update";
    public static final String UPDATE_PASSWORD = "update-secret";
    public static final String ADMIN_USER = "vm_admin";
    public static final String ADMIN_PASSWORD = "admin-secret";

    private static TestStore shared;

    private final FusekiServer server;

    private TestStore() {
        try {
            Path dir = Files.createTempDirectory("vm-store");
            String config = Files.readString(Path.of("../fuseki/config.ttl"), StandardCharsets.UTF_8)
                    .replace("@READ@", READ_USER)
                    .replace("@UPDATE@", UPDATE_USER)
                    .replace("@ADMIN@", ADMIN_USER)
                    .replace("/fuseki/databases/vocabulary", "--mem--");
            Path configFile = dir.resolve("config.ttl");
            Files.writeString(configFile, config);
            Path passwd = dir.resolve("passwd");
            Files.writeString(
                    passwd,
                    READ_USER + ": " + READ_PASSWORD + "\n" + UPDATE_USER + ": " + UPDATE_PASSWORD + "\n" + ADMIN_USER
                            + ": " + ADMIN_PASSWORD + "\n");
            server = FusekiServer.create()
                    .port(0)
                    .passwordFile(passwd.toString())
                    .auth(AuthScheme.BASIC)
                    .parseConfigFile(configFile)
                    .build()
                    .start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The store shared by all tests of a JVM. */
    public static synchronized TestStore shared() {
        if (shared == null) {
            shared = new TestStore();
        }
        return shared;
    }

    public String url() {
        return "http://localhost:" + server.getHttpPort();
    }
}
