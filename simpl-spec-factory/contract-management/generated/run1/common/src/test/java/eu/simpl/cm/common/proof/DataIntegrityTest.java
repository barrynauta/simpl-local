package eu.simpl.cm.common.proof;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/** Appendix E: keys, DIDs and the worked vector. */
class DataIntegrityTest {
    private static final ObjectMapper M = new ObjectMapper();

    static byte[] seed(int b) {
        byte[] s = new byte[32];
        java.util.Arrays.fill(s, (byte) b);
        return s;
    }

    static final String VECTOR = """
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
        }""";

    @Test
    void keysAndDidsMatchAppendixE1() {
        assertThat(HexFormat.of().formatHex(DataIntegrity.publicKeyFromSeed(seed(0x11))))
                .isEqualTo("d04ab232742bb4ab3a1368bd4615e4e6d0224ab71a016baf8520a332c9778737");
        assertThat(DidKey.did(DataIntegrity.publicKeyFromSeed(seed(0x11))))
                .isEqualTo("did:key:z6MktULudTtAsAhRegYPiZ6631RV3viv12qd4GQF8z1xB22S");
        assertThat(DidKey.did(DataIntegrity.publicKeyFromSeed(seed(0x22))))
                .isEqualTo("did:key:z6MkqGC3nWZhYieEVTVDKW5v588CiGfsDSmRVG9ZwwWTvLSK");
        assertThat(DidKey.did(DataIntegrity.publicKeyFromSeed(seed(0x33))))
                .isEqualTo("did:key:z6Mkg49NtQR2LyYRDCQFK4w1VVHqhypZSSRo7HsyuN7SV7v5");
        assertThat(DidKey.publicKey("did:key:z6MktULudTtAsAhRegYPiZ6631RV3viv12qd4GQF8z1xB22S"))
                .isEqualTo(DataIntegrity.publicKeyFromSeed(seed(0x11)));
    }

    @Test
    void usageContractHashOfVector() {
        assertThat(DataIntegrity.sha256Hex("<p>x</p>".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("31d8e07ec305ac4e2515d1f0b1c8c603b3858044eb7d97807d6d0970838cb0be");
    }

    @Test
    void reproducesWorkedVectorProofValue() throws Exception {
        ObjectNode doc = (ObjectNode) M.readTree(VECTOR);
        ObjectNode signed = DataIntegrity.sign(doc, seed(0x11), "2026-10-29T10:00:00Z");
        assertThat(signed.at("/proof/proofValue").asText())
                .isEqualTo("z4wkqX9F8vbz1ww6tHENYSuN874yUNAYS27thqUbxBaKbFdzg8cRuYN3ppciKXSsxfax54nf2JEVCfj2n7MDYi3gC");
        assertThat(signed.at("/proof/verificationMethod").asText())
                .isEqualTo("did:key:z6MktULudTtAsAhRegYPiZ6631RV3viv12qd4GQF8z1xB22S#z6MktULudTtAsAhRegYPiZ6631RV3viv12qd4GQF8z1xB22S");
        assertThat(signed.get("proof").has("@context")).isFalse();
        assertThat(DataIntegrity.verify(signed)).isEqualTo(DataIntegrity.Outcome.VALID);
    }

    @Test
    void tamperedSubjectOrProofValueIsInvalid() throws Exception {
        ObjectNode signed = DataIntegrity.sign((ObjectNode) M.readTree(VECTOR), seed(0x11), "2026-10-29T10:00:00Z");
        ObjectNode t1 = signed.deepCopy();
        ((ObjectNode) t1.get("credentialSubject")).put("assetId", "asset-2");
        assertThat(DataIntegrity.verify(t1)).isEqualTo(DataIntegrity.Outcome.INVALID_SIGNATURE);
        ObjectNode t2 = signed.deepCopy();
        String pv = t2.at("/proof/proofValue").asText();
        char last = pv.charAt(pv.length() - 1);
        ((ObjectNode) t2.get("proof")).put("proofValue", pv.substring(0, pv.length() - 1) + (last == 'C' ? 'D' : 'C'));
        assertThat(DataIntegrity.verify(t2)).isEqualTo(DataIntegrity.Outcome.INVALID_SIGNATURE);
        ObjectNode t3 = signed.deepCopy();
        t3.remove("proof");
        assertThat(DataIntegrity.verify(t3)).isEqualTo(DataIntegrity.Outcome.MALFORMED);
    }

    @Test
    void jcsSortsKeysAndEscapes() throws Exception {
        assertThat(Jcs.canonicalize(M.readTree("{\"b\":\"\\u00e9\\n\",\"a\":[\"x\",{\"d\":1,\"c\":true}]}")))
                .isEqualTo("{\"a\":[\"x\",{\"c\":true,\"d\":1}],\"b\":\"é\\n\"}");
    }

    @Test
    void base58RoundTrip() {
        byte[] data = {0, 0, 1, 2, (byte) 255};
        assertThat(Base58.decode(Base58.encode(data))).isEqualTo(data);
    }
}
