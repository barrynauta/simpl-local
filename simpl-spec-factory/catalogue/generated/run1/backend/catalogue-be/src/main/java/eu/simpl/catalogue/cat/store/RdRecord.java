package eu.simpl.catalogue.cat.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.common.Json;

import java.time.Instant;

/** A stored resource description version. */
public record RdRecord(String id, String sdHash, String issuer, String providedBy, String status,
                       long uploadMs, long statusMs, String resourceKey, int version, String schemaName,
                       String content, String qar, String quality, Double overallScore, String overallClass) {

    public JsonNode vc() { return Json.parse(content); }

    public boolean active() { return "active".equals(status); }

    /** SelfDescription meta of the composed contract, plus qualityAssessment when present (D18). */
    public ObjectNode meta() {
        ObjectNode m = Json.object();
        m.put("sdHash", sdHash);
        m.put("id", id);
        m.put("status", status);
        m.put("issuer", issuer);
        m.putArray("validatorDids");
        m.put("uploadDatetime", Instant.ofEpochMilli(uploadMs).toString());
        m.put("statusDatetime", Instant.ofEpochMilli(statusMs).toString());
        m.put("resourceKey", resourceKey);
        m.put("currentVersion", version);
        if (quality != null) m.set("qualityAssessment", Json.parse(quality));
        return m;
    }
}
