package eu.simpl.catalogue.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Main properties of D34, taken from the stored VC. */
public final class RdProjection {
    private RdProjection() {}

    public static ObjectNode mainProperties(JsonNode vc) {
        JsonNode cs = vc.path("credentialSubject");
        JsonNode g = cs.path("simpl:generalServiceProperties");
        if (g.isArray()) g = g.path(0);
        ObjectNode n = Json.object();
        n.putArray("claimsGraphUri").add(cs.path("@id").asText());
        n.put("offeringType", Json.literal(g.get("simpl:offeringType")));
        n.put("name", Json.literal(g.get("simpl:name")));
        n.put("inLanguage", Json.literal(g.get("simpl:inLanguage")));
        n.put("description", Json.literal(g.get("simpl:description")));
        n.put("serviceAccessPoint", Json.literal(g.get("simpl:serviceAccessPoint")));
        return n;
    }

    public static String subjectId(JsonNode vc) {
        return vc.path("credentialSubject").path("@id").asText(null);
    }

    public static String providedBy(JsonNode vc) {
        JsonNode p = vc.path("credentialSubject").path("simpl:providerInformation");
        if (p.isArray()) p = p.path(0);
        return Json.literal(p.get("simpl:providedBy"));
    }

    /** Local name of the subject type, for example DataOffering. */
    public static String resourceType(JsonNode vc) {
        JsonNode cs = vc.path("credentialSubject");
        JsonNode t = cs.has("rdf:type") ? cs.get("rdf:type") : cs.get("@type");
        String s = Json.literal(t);
        return s == null ? "" : AdvancedQuery.localName(s);
    }
}
