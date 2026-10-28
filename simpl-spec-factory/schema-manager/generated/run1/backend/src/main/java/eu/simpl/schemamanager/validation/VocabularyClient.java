package eu.simpl.schemamanager.validation;

import eu.simpl.schemamanager.config.Settings;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Client of the Vocabulary Manager contract of D60:
 * GET {VOCABULARY_MANAGER_URL}/vocabularies/{name}/content, no token.
 */
@Component
public class VocabularyClient {

    private static final Logger LOG = LoggerFactory.getLogger(VocabularyClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient http;
    private final Settings settings;

    public VocabularyClient(HttpClient http, Settings settings) {
        this.http = http;
        this.settings = settings;
    }

    /**
     * Returns the Turtle content of a vocabulary, or empty when the Vocabulary Manager
     * answers 404 (or another 4xx). Throws when it is unreachable, answers 5xx or times out.
     */
    public Optional<byte[]> fetch(String vocabularyName) {
        String base = settings.vocabularyManagerUrl().replaceAll("/+$", "");
        URI uri = URI.create(base + "/vocabularies/"
                + URLEncoder.encode(vocabularyName, StandardCharsets.UTF_8).replace("+", "%20") + "/content");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("Accept", "text/turtle")
                .GET()
                .build();
        HttpResponse<byte[]> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            LOG.warn("Vocabulary manager unreachable at {}: {}", uri, e.toString());
            throw new VocabularyUnavailableException("vocabulary manager unreachable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VocabularyUnavailableException("vocabulary manager unreachable", e);
        }
        int code = response.statusCode();
        if (code >= 200 && code < 300) {
            return Optional.of(response.body());
        }
        if (code >= 500) {
            LOG.warn("Vocabulary manager answered {} for {}", code, uri);
            throw new VocabularyUnavailableException("vocabulary manager answered " + code, null);
        }
        return Optional.empty();
    }
}
