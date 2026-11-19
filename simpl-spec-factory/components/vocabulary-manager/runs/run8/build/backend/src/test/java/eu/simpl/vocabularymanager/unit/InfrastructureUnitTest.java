package eu.simpl.vocabularymanager.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import eu.simpl.vocabularymanager.config.TomcatConfig;
import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.security.IdentityProviderUnavailableException;
import eu.simpl.vocabularymanager.security.RealmKeySource;
import eu.simpl.vocabularymanager.security.TokenValidator;
import eu.simpl.vocabularymanager.support.TestEnvironment;
import eu.simpl.vocabularymanager.web.routing.ApiOperation;
import eu.simpl.vocabularymanager.web.routing.OperationCatalogue;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

/** Unit tests of the routing catalogue and default deny (D84), the key source (D44) and the Tomcat error body. */
class InfrastructureUnitTest {

    @Test
    void everyOperationHasARoleRuleAndUnknownRequestsResolveToNothing() {
        OperationCatalogue catalogue = new OperationCatalogue();
        assertThat(catalogue.operations()).hasSizeGreaterThanOrEqualTo(16);
        for (ApiOperation operation : catalogue.operations()) {
            assertThat(operation.access()).as(operation.path()).isNotNull();
        }
        assertThat(catalogue.resolve("/v1/vocabularies", "GET")).isPresent();
        assertThat(catalogue.resolve("/v1/vocabularies/searches", "POST").orElseThrow().path())
                .isEqualTo("/v1/vocabularies/searches");
        assertThat(catalogue.resolve("/v1/vocabularies", "PUT")).isEmpty();
        assertThat(catalogue.resolve("/v1/unknown", "GET")).isEmpty();
        assertThat(catalogue.resolve("/vocabularies", "GET")).isEmpty();
        assertThat(catalogue.resolve("/v1/health", "GET").orElseThrow().access()).isEqualTo(ApiOperation.Access.PUBLIC);
        assertThat(catalogue.resolve("/v1/vocabularies", "POST").orElseThrow().access())
                .isEqualTo(ApiOperation.Access.WRITE);
        assertThat(catalogue.resolve("/v1/external-vocabulary/searches", "POST").orElseThrow().access())
                .isEqualTo(ApiOperation.Access.READ);
        assertThat(catalogue.matchPath("/v1/vocabularies/Abc/versions/1")).isPresent();
    }

    /** A handler method to register in a fake mapping. */
    public void handler() {
        // used by reflection only
    }

    @Test
    void handlerWithoutRuleIsReported() throws Exception {
        OperationCatalogue catalogue = new OperationCatalogue();
        Method method = InfrastructureUnitTest.class.getMethod("handler");
        RequestMappingInfo extra = RequestMappingInfo.paths("/v1/secret").methods(RequestMethod.GET).build();
        Set<String> problems = mismatches(Map.of(extra, new HandlerMethod(this, method)), catalogue);
        assertThat(problems).anyMatch(problem -> problem.contains("handler without rule"));
        assertThat(problems).anyMatch(problem -> problem.contains("rule without handler"));
        RequestMappingInfo noMethod = RequestMappingInfo.paths("/v1/any").build();
        assertThat(mismatches(Map.of(noMethod, new HandlerMethod(this, method)), catalogue))
                .anyMatch(problem -> problem.contains("handler without method"));
    }

    @SuppressWarnings("unchecked")
    private static Set<String> mismatches(Map<RequestMappingInfo, HandlerMethod> handlers, OperationCatalogue catalogue)
            throws Exception {
        Method mismatches = eu.simpl.vocabularymanager.config.HandlerRuleVerifier.class.getDeclaredMethod(
                "mismatches", Map.class, OperationCatalogue.class);
        mismatches.setAccessible(true);
        return (Set<String>) mismatches.invoke(null, handlers, catalogue);
    }

    @Test
    void keySourceFailsClosedAndRecovers() throws Exception {
        AtomicInteger status = new AtomicInteger(503);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        TestEnvironment env = TestEnvironment.get();
        server.createContext("/certs", exchange -> {
            byte[] body = status.get() == 200
                    ? ("{\"keys\":[" + env.key().toPublicJWK().toJSONString() + "]}").getBytes(StandardCharsets.UTF_8)
                    : "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        try {
            var identity = new VocabularyManagerProperties.Identity(TestEnvironment.ISSUER,
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/certs", TestEnvironment.AUDIENCE,
                    Duration.ofSeconds(2), Duration.ZERO);
            RealmKeySource source = new RealmKeySource(HttpClient.newHttpClient(), identity, Clock.systemUTC());
            source.loadAtStart();
            assertThat(source.hasKeys()).isFalse();
            TokenValidator validator = new TokenValidator(source, TestEnvironment.ISSUER, TestEnvironment.AUDIENCE);
            assertThatThrownBy(() -> validator.validate(env.admin()))
                    .isInstanceOf(IdentityProviderUnavailableException.class);
            status.set(200);
            assertThat(validator.validate(env.admin()).roles()).contains(TestEnvironment.ADMIN_ROLE);
            assertThat(source.hasKeys()).isTrue();
            status.set(500);
            // Cached keys keep working while the identity provider is down.
            assertThat(validator.validate(env.viewer()).subject()).isEqualTo("sub-ga-viewer");
            assertThatThrownBy(() -> validator.validate(env.tokenWithUnknownKey()))
                    .isInstanceOf(IdentityProviderUnavailableException.class);
            assertThatThrownBy(() -> validator.validate("abc.def"))
                    .isInstanceOf(TokenValidator.InvalidTokenException.class);
        } finally {
            server.stop(0);
        }
        var unreachable = new VocabularyManagerProperties.Identity(TestEnvironment.ISSUER, "http://127.0.0.1:9/certs",
                TestEnvironment.AUDIENCE, Duration.ofMillis(500), Duration.ZERO);
        RealmKeySource down = new RealmKeySource(HttpClient.newHttpClient(), unreachable, Clock.systemUTC());
        assertThatThrownBy(down::refresh).isInstanceOf(IdentityProviderUnavailableException.class);
    }

    @Test
    void tomcatErrorBodiesAreProblems() throws Exception {
        Method body = TomcatConfig.ProblemErrorReportValve.class.getDeclaredMethod("problemBody", int.class);
        body.setAccessible(true);
        assertThat((String) body.invoke(null, 400)).contains("urn:problem-type:belgif:badRequest");
        assertThat((String) body.invoke(null, 413)).contains("payloadTooLarge");
        assertThat((String) body.invoke(null, 500)).contains("internalServerError");
        assertThat((String) body.invoke(null, 404)).contains("\"status\":404");
    }
}
