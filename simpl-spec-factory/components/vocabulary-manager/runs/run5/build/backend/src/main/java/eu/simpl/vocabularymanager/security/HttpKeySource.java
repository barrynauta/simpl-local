package eu.simpl.vocabularymanager.security;

import com.nimbusds.jose.jwk.JWKSet;
import eu.simpl.vocabularymanager.config.VocabularyProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** Reads the JWKS endpoint of the Keycloak realm with a 2 s timeout and no retry (D44). */
@Component
public class HttpKeySource implements KeySource {

    private final HttpClient http;
    private final URI jwksUri;
    private final Duration timeout;

    public HttpKeySource(VocabularyProperties properties) {
        this.timeout = properties.identity().fetchTimeout();
        this.jwksUri = URI.create(properties.identity().jwksUrl());
        this.http = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public JWKSet fetch() {
        HttpRequest request = HttpRequest.newBuilder(jwksUri)
                .timeout(timeout)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IdentityProviderUnavailableException(
                        "Realm keys answered with status " + response.statusCode(), null);
            }
            return JWKSet.parse(response.body());
        } catch (IOException | ParseException e) {
            throw new IdentityProviderUnavailableException("Realm keys could not be fetched", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IdentityProviderUnavailableException("Interrupted while fetching realm keys", e);
        }
    }
}
