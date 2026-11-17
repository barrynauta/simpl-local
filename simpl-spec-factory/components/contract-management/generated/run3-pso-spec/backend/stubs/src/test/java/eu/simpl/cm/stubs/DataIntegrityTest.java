package eu.simpl.cm.stubs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.cm.stubs.crypto.DataIntegrity;
import eu.simpl.cm.stubs.crypto.Ed25519Key;
import eu.simpl.cm.stubs.crypto.Jcs;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/** Appendix E: keys, DIDs and the worked vector. */
class DataIntegrityTest {

    private static final ObjectMapper M = new ObjectMapper();

    private static final String VECTOR = """
            {
              "@context": ["https://www.w3.org/ns/credentials/v2"],
              "id": "urn:uuid:00000000-0000-4000-8000-000000000001",
              "type": ["VerifiableCredential", "UsageContractCredential"],
              "issuer": "did:key:z6MktULudTtAsAhRegYPiZ6631RV3viv12qd4GQF8z1xB22S",
              "validFrom": "2026-10-29T10:00:00Z",
              "validUntil": "2027-10-29T10:00:00Z",
              "credentialSubject": {
                "id": "urn:simpl:contract:9e82cdd6-4aba-4a77-8dcc-f1a9f56d82c5",
                "contractNegotiationId": "9e82cdd6-4aba-4a77-8dcc-f1a9f56d82c5",
                "contractAgreementId": "677186f6-1179-4cf1-b845-68d1c00c185f",
                "contractDefinitionId": "def-1",
                "assetId": "asset-1",
                "resourceDescriptionId": "rd-1",
                "providerId": "provider-01",
                "consumerId": "consumer-01",
                "usageContractHash": "31d8e07ec305ac4e2515d1f0b1c8c603b3858044eb7d97807d6d0970838cb0be",
                "signerRole": "PROVIDER",
                "signedAt": "2026-10-29T10:00:00Z"
              }
            }
            """;

    @Test
    void keysMatchAppendixE1() {
        Ed25519Key p = Ed25519Key.fromRepeatedByte(0x11);
        Ed25519Key c = Ed25519Key.fromRepeatedByte(0x22);
        Ed25519Key r = Ed25519Key.fromRepeatedByte(0x33);
        assertThat(p.publicKeyHex()).isEqualTo("d04ab232742bb4ab3a1368bd4615e4e6d0224ab71a016baf8520a332c9778737");
        assertThat(p.did()).isEqualTo("did:key:z6MktULudTtAsAhRegYPiZ6631RV3viv12qd4GQF8z1xB22S");
        assertThat(c.did()).isEqualTo("did:key:z6MkqGC3nWZhYieEVTVDKW5v588CiGfsDSmRVG9ZwwWTvLSK");
        assertThat(r.did()).isEqualTo("did:key:z6Mkg49NtQR2LyYRDCQFK4w1VVHqhypZSSRo7HsyuN7SV7v5");
        assertThat(HexFormat.of().formatHex(Ed25519Key.decodeMultibase(c.multibase())))
                .isEqualTo("a09aa5f47a6759802ff955f8dc2d2a14a5c99d23be97f864127ff9383455a4f0");
    }

    @Test
    void usageContractHashOfVector() {
        assertThat(HexFormat.of().formatHex(DataIntegrity.sha256("<p>x</p>".getBytes(StandardCharsets.UTF_8))))
                .isEqualTo("31d8e07ec305ac4e2515d1f0b1c8c603b3858044eb7d97807d6d0970838cb0be");
    }

    @Test
    void reproducesWorkedVectorProofValue() throws Exception {
        ObjectNode vc = (ObjectNode) M.readTree(VECTOR);
        ObjectNode signed = DataIntegrity.sign(vc, Ed25519Key.fromRepeatedByte(0x11), "2026-10-29T10:00:00Z");
        assertThat(signed.at("/proof/proofValue").asText()).isEqualTo(
                "z4wkqX9F8vbz1ww6tHENYSuN874yUNAYS27thqUbxBaKbFdzg8cRuYN3ppciKXSsxfax54nf2JEVCfj2n7MDYi3gC");
        assertThat(signed.at("/proof/verificationMethod").asText()).isEqualTo(
                "did:key:z6MktULudTtAsAhRegYPiZ6631RV3viv12qd4GQF8z1xB22S#z6MktULudTtAsAhRegYPiZ6631RV3viv12qd4GQF8z1xB22S");
        assertThat(DataIntegrity.verifySignature(signed)).isTrue();
    }

    @Test
    void tamperedSubjectOrProofValueFailsVerification() throws Exception {
        ObjectNode vc = (ObjectNode) M.readTree(VECTOR);
        ObjectNode signed = DataIntegrity.sign(vc, Ed25519Key.fromRepeatedByte(0x11), "2026-10-29T10:00:00Z");
        ObjectNode tampered = signed.deepCopy();
        ((ObjectNode) tampered.get("credentialSubject")).put("assetId", "asset-2");
        assertThat(DataIntegrity.verifySignature(tampered)).isFalse();
        ObjectNode badProof = signed.deepCopy();
        String pv = badProof.at("/proof/proofValue").asText();
        ((ObjectNode) badProof.get("proof")).put("proofValue", pv.substring(0, pv.length() - 1)
                + (pv.endsWith("C") ? "D" : "C"));
        assertThat(DataIntegrity.verifySignature(badProof)).isFalse();
    }

    @Test
    void jcsSortsKeysAndOmitsWhitespace() throws Exception {
        String canon = new String(Jcs.canonicalize(M.readTree("{\"b\": [\"x\", \"é\"], \"a\": \"q\\\"\"}")),
                StandardCharsets.UTF_8);
        assertThat(canon).isEqualTo("{\"a\":\"q\\\"\",\"b\":[\"x\",\"é\"]}");
    }
}
