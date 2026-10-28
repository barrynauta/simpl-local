package eu.simpl.catalogue.cat.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.catalogue.common.Json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Static participant key registry of D23: participant_id to Ed25519 public keys (x). */
public class KeyRegistry {
    private final Map<String, List<String>> keys = new HashMap<>();

    public static KeyRegistry load(String file) {
        KeyRegistry r = new KeyRegistry();
        if (file == null || file.isBlank()) return r;
        try {
            JsonNode n = Json.MAPPER.readTree(Files.readString(Path.of(file)));
            n.fields().forEachRemaining(e -> {
                List<String> xs = new ArrayList<>();
                e.getValue().forEach(x -> xs.add(DidJwk.stripPadding(x.asText())));
                r.keys.put(e.getKey(), xs);
            });
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read key registry " + file, e);
        }
        return r;
    }

    public void register(String participant, String x) {
        keys.computeIfAbsent(participant, k -> new ArrayList<>()).add(DidJwk.stripPadding(x));
    }

    public boolean isRegistered(String participant, String x) {
        return participant != null && x != null && keys.getOrDefault(participant, List.of()).contains(x);
    }
}
