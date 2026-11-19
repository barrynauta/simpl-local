package eu.simpl.vocabulary.security;

import com.nimbusds.jose.jwk.JWKSet;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.time.Duration;

/** Fetches the realm JWK set over HTTP with a short timeout and no retry (D44). */
public class HttpKeyFetcher implements RealmKeySource.KeyFetcher {

    private static final int MAX_BYTES = 256 * 1024;

    private final HttpClient client;
    private final URI jwksUri;
    private final Duration timeout;

    public HttpKeyFetcher(URI jwksUri, Duration timeout) {
        this.jwksUri = jwksUri;
        this.timeout = timeout;
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public JWKSet fetch() throws IOException {
        HttpRequest request = HttpRequest.newBuilder(jwksUri)
                .timeout(timeout)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<byte[]> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching realm keys", e);
        }
        if (response.statusCode() != 200) {
            throw new IOException("Realm keys endpoint answered " + response.statusCode());
        }
        byte[] body = response.body();
        if (body.length > MAX_BYTES) {
            throw new IOException("Realm key set too large");
        }
        try {
            return JWKSet.parse(new String(body, java.nio.charset.StandardCharsets.UTF_8));
        } catch (ParseException e) {
            throw new IOException("Realm key set could not be parsed", e);
        }
    }
}
