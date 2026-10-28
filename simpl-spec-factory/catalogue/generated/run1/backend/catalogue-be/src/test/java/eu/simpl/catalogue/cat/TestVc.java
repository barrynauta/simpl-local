package eu.simpl.catalogue.cat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.cat.crypto.DidJwk;
import eu.simpl.catalogue.cat.crypto.Jcs;
import eu.simpl.catalogue.common.Json;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

/** Builds and signs test RDs exactly as digest D22, D23 and section 6.7 describe. */
public final class TestVc {
    private TestVc() {}

    public static Ed25519PrivateKeyParameters key(String name) {
        try {
            byte[] seed = MessageDigest.getInstance("SHA-256")
                    .digest(("simpl-catalogue-factory/" + name).getBytes(StandardCharsets.US_ASCII));
            return new Ed25519PrivateKeyParameters(seed, 0);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String x(String name) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(key(name).generatePublicKey().getEncoded());
    }

    public static String did(String name) {
        return DidJwk.fromX(x(name));
    }

    public static ObjectNode unsigned(String keyName, String providedBy, String token, String assignee) {
        String id = "did:web:test.simpl.local:DataOffering:" + UUID.randomUUID();
        String policy = "{\\\"permission\\\":[{\\\"action\\\":[\\\"http://simpl.eu/odrl/actions/search\\\"],\\\"assignee\\\":{\\\"uid\\\":\\\"" + assignee + "\\\"}}]}";
        return (ObjectNode) Json.parse("""
            {"@context": ["https://www.w3.org/2018/credentials/v1", "https://w3id.org/security/suites/jws-2020/v1"],
             "type": "VerifiableCredential", "issuer": "%s", "issuanceDate": "2026-10-28T10:00:00Z",
             "credentialSubject": {
               "@context": {"simpl": "http://w3id.org/gaia-x/simpl#", "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                            "xsd": "http://www.w3.org/2001/XMLSchema#", "dct": "http://purl.org/dc/terms/"},
               "@id": "%s", "rdf:type": {"@id": "simpl:DataOffering"},
               "dct:conformsTo": {"@id": "urn:simpl:schema:DataOfferingShape:1", "dct:schemaName": "DataOfferingShape", "dct:hasVersion": "1"},
               "simpl:version": 1,
               "simpl:generalServiceProperties": {"rdf:type": {"@id": "simpl:GeneralServiceProperties"},
                 "simpl:name": "%s dataset", "simpl:description": "%s description", "simpl:inLanguage": "en",
                 "simpl:offeringType": "data",
                 "simpl:serviceAccessPoint": {"@type": "xsd:anyURI", "@value": "https://provider.example.org/data"}},
               "simpl:providerInformation": {"rdf:type": {"@id": "simpl:ProviderInformation"}, "simpl:providedBy": "%s"},
               "simpl:offeringPrice": {"rdf:type": {"@id": "simpl:OfferingPrice"},
                 "simpl:price": {"@type": "xsd:decimal", "@value": 100}, "simpl:currency": "EUR"},
               "simpl:servicePolicy": {"rdf:type": {"@id": "simpl:ServicePolicy"}, "simpl:access-policy": "%s"}
             }}""".formatted(did(keyName), id, token, token, providedBy, policy));
    }

    public static ObjectNode sign(ObjectNode vc, String keyName) {
        ObjectNode unsigned = vc.deepCopy();
        unsigned.remove("proof");
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"EdDSA\",\"b64\":false,\"crit\":[\"b64\"]}".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream in = new ByteArrayOutputStream();
        in.writeBytes((header + ".").getBytes(StandardCharsets.US_ASCII));
        in.writeBytes(Jcs.canonical(unsigned));
        Ed25519Signer s = new Ed25519Signer();
        s.init(true, key(keyName));
        byte[] data = in.toByteArray();
        s.update(data, 0, data.length);
        String sig = Base64.getUrlEncoder().withoutPadding().encodeToString(s.generateSignature());
        ObjectNode proof = unsigned.putObject("proof");
        proof.put("type", "JsonWebSignature2020");
        proof.put("proofPurpose", "assertionMethod");
        proof.put("created", "2026-10-28T10:00:00Z");
        proof.put("verificationMethod", unsigned.get("issuer").asText() + "#0");
        proof.put("jws", header + ".." + sig);
        return unsigned;
    }

    public static ObjectNode signed(String keyName, String providedBy, String token, String assignee) {
        return sign(unsigned(keyName, providedBy, token, assignee), keyName);
    }

    public static String id(JsonNode vc) {
        return vc.path("credentialSubject").path("@id").asText();
    }
}
