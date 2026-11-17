package eu.simpl.cm.manager.service;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.cm.manager.client.HttpResult;
import eu.simpl.cm.manager.client.StubClient;
import org.springframework.stereotype.Component;

/** Reads the participant directory and the catalogue from the stub (C.5). */
@Component
public class Directory {

    public record Participant(String participantId, String name, String did, String contactEmail) {
    }

    private final StubClient stub;

    public Directory(StubClient stub) {
        this.stub = stub;
    }

    /** The participant, or null when the directory does not know it or cannot be reached. */
    public Participant participant(String nid, String participantId) {
        if (participantId == null) {
            return null;
        }
        HttpResult r = stub.participant(nid, participantId);
        if (r.status() != 200 || r.body() == null) {
            return null;
        }
        JsonNode b = r.body();
        return new Participant(text(b, "participantId"), text(b, "name"), text(b, "did"), text(b, "contactEmail"));
    }

    public String name(String nid, String participantId) {
        Participant p = participant(nid, participantId);
        return p == null ? null : p.name();
    }

    static String text(JsonNode n, String field) {
        JsonNode v = n == null ? null : n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }
}
