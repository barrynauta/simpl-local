package eu.simpl.catalogue.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/** OAuth2 client-credentials token with caching (D36: the Trusted qma gets a federation token from its realm). */
public class TokenClient {
    private final FederationConfig.Client client;
    private final RestClient http = Http.client(Duration.ofSeconds(10));
    private String token;
    private Instant expires = Instant.EPOCH;

    public TokenClient(FederationConfig.Client client) {
        this.client = client;
    }

    public synchronized String token() {
        if (token != null && Instant.now().isBefore(expires)) return token;
        String form = "grant_type=client_credentials&client_id=" + enc(client.clientId)
                + "&client_secret=" + enc(client.clientSecret);
        JsonNode res = http.post().uri(client.tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
        if (res == null || !res.hasNonNull("access_token")) throw new IllegalStateException("No access token");
        token = res.get("access_token").asText();
        long ttl = res.path("expires_in").asLong(60);
        expires = Instant.now().plusSeconds(Math.max(5, ttl - 30));
        return token;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
