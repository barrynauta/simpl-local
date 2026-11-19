package eu.simpl.vocabularymanager.support;

import eu.simpl.vocabularymanager.VocabularyManagerApplication;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/** An extra instance of the whole service, for multi-instance (D82) and failure (D44) tests. */
public final class Instance implements AutoCloseable {

    private final ConfigurableApplicationContext context;

    private Instance(ConfigurableApplicationContext context) {
        this.context = context;
    }

    /**
     * @param storeUrl the store URL
     * @param overrides extra properties as key=value
     * @return the started instance
     */
    public static Instance start(String storeUrl, String... overrides) {
        TestEnvironment env = TestEnvironment.get();
        List<String> args = new ArrayList<>(List.of(
                "--server.port=0",
                "--management.server.port=" + freePort(),
                "--vocabulary-manager.store.url=" + storeUrl,
                "--vocabulary-manager.store.read-password=unused",
                "--vocabulary-manager.store.update-password=unused",
                "--vocabulary-manager.identity.issuer-uri=" + TestEnvironment.ISSUER,
                "--vocabulary-manager.identity.jwks-uri=" + env.jwksUrl(),
                "--vocabulary-manager.content-base-url=https://authority.example/vocabulary-manager"));
        for (String override : overrides) {
            args.add("--" + override);
        }
        ConfigurableApplicationContext context = new SpringApplicationBuilder(VocabularyManagerApplication.class)
                .run(args.toArray(String[]::new));
        return new Instance(context);
    }

    /**
     * @return a client for this instance
     */
    public Api api() {
        int port = ((WebServerApplicationContext) context).getWebServer().getPort();
        return new Api(port);
    }

    @Override
    public void close() {
        context.close();
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
