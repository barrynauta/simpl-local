package eu.simpl.cm.stubs;

import eu.simpl.cm.stubs.crypto.Ed25519Key;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The known participants: keys (Appendix E.1), directory entries (C.5) and the CM each one runs.
 * Keys live only in the stub (D54).
 */
@Component
public class Participants {

    public record Participant(String id, String name, String contactEmail, Ed25519Key key, boolean trusted,
                              String cmUrl, String cmApiKey) {
        public String did() {
            return key.did();
        }
    }

    private final Map<String, Participant> byId = new LinkedHashMap<>();

    public Participants(@Value("${stub.provider.cm-url}") String providerCm,
                        @Value("${stub.provider.cm-key}") String providerKey,
                        @Value("${stub.consumer.cm-url}") String consumerCm,
                        @Value("${stub.consumer.cm-key}") String consumerKey) {
        add(new Participant("provider-01", "Provider One", "provider-01@example.test",
                Ed25519Key.fromRepeatedByte(0x11), true, providerCm, providerKey));
        add(new Participant("consumer-01", "Consumer One", "consumer-01@example.test",
                Ed25519Key.fromRepeatedByte(0x22), true, consumerCm, consumerKey));
        add(new Participant("rogue-01", "Rogue Participant", null,
                Ed25519Key.fromRepeatedByte(0x33), false, null, null));
    }

    private void add(Participant p) {
        byId.put(p.id(), p);
    }

    public Participant get(String id) {
        return id == null ? null : byId.get(id);
    }

    public Participant byDid(String did) {
        return byId.values().stream().filter(p -> p.did().equals(did)).findFirst().orElse(null);
    }

    /** The counterparty of a participant in a two-party run. */
    public Participant counterparty(String id) {
        if ("provider-01".equals(id)) {
            return byId.get("consumer-01");
        }
        if ("consumer-01".equals(id)) {
            return byId.get("provider-01");
        }
        return null;
    }
}
