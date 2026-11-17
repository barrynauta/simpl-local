package eu.simpl.cm.manager.service;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.cm.common.web.HttpJson;
import eu.simpl.cm.manager.clients.StubClient;
import eu.simpl.cm.manager.domain.ContractRecord;
import org.springframework.stereotype.Component;

/** Reads catalogue and directory data for a record. Failures leave fields empty. */
@Component
public class Enricher {
    private final StubClient stub;

    public Enricher(StubClient stub) {
        this.stub = stub;
    }

    public JsonNode asset(String nid, String assetId) {
        HttpJson.Response r = stub.asset(nid, assetId);
        return r.status() == 200 ? stub.json(r) : null;
    }

    public String participantName(String nid, String participantId) {
        if (participantId == null) {
            return null;
        }
        HttpJson.Response r = stub.participant(nid, participantId);
        if (r.status() != 200) {
            return null;
        }
        JsonNode j = stub.json(r);
        return j == null || !j.path("name").isTextual() ? null : j.get("name").asText();
    }

    /** Returns the DID, null if the participant is unknown; throws on a technical error. */
    public String participantDid(String nid, String participantId) {
        HttpJson.Response r = stub.participant(nid, participantId);
        if (r.status() == 404) {
            return null;
        }
        if (r.status() != 200) {
            throw new CredentialValidator.TechnicalError("Directory unavailable");
        }
        JsonNode j = stub.json(r);
        return j == null || !j.path("did").isTextual() ? null : j.get("did").asText();
    }

    public void enrich(ContractRecord r) {
        try {
            JsonNode asset = asset(r.contractNegotiationId, r.assetId);
            if (asset != null) {
                applyAsset(r, asset);
            } else if (r.consentStatus == null) {
                r.consentStatus = "NOT_DEFINED";
            }
            if (r.providerName == null) {
                r.providerName = participantName(r.contractNegotiationId, r.providerId);
            }
            r.consumerName = participantName(r.contractNegotiationId, r.consumerId);
        } catch (RuntimeException e) {
            // D19: enrichment failure does not fail the call
        }
        if (r.consentStatus == null) {
            r.consentStatus = "NOT_DEFINED";
        }
    }

    public static void applyAsset(ContractRecord r, JsonNode asset) {
        r.resourceDescriptionId = text(asset, "resourceDescriptionId");
        r.resourceName = text(asset, "resourceName");
        r.providerName = text(asset, "providerName");
        r.consentStatus = asset.path("consentRequired").asBoolean(false) ? "PENDING" : "NOT_DEFINED";
    }

    private static String text(JsonNode n, String f) {
        JsonNode v = n.get(f);
        return v == null || v.isNull() ? null : v.asText();
    }
}
