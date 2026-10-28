package eu.simpl.catalogue.cat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.cat.crypto.DidJwk;
import eu.simpl.catalogue.cat.crypto.Jcs;
import eu.simpl.catalogue.cat.crypto.SignatureVerifier;
import eu.simpl.catalogue.common.Json;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoTest {
    @Test
    void fixedSeedsGiveTheKeysOfTheDigest() {
        assertThat(TestVc.x("provider-a")).isEqualTo("MSC615MULPOxkuLnq2RlmNcxMU8fvDfq_Iy6vXVhEPY");
        assertThat(TestVc.x("provider-a2")).isEqualTo("fDG5arCWrkwpREKH_CfxIxNgyCM1FZ97GPkheLg35Ls");
        assertThat(TestVc.x("provider-b")).isEqualTo("7Hu7c6wpBAUBnCvhn_UVU9XTPQqPUuIDi0NdBLpi81U");
        assertThat(TestVc.x("unregistered")).isEqualTo("WtN5__UEhdz_e6ANP8dYx_lKAxxxEkynOtWbTFGczm8");
        assertThat(DidJwk.publicX(TestVc.did("provider-a") + "#0")).isEqualTo(TestVc.x("provider-a"));
    }

    @Test
    void jcsSortsKeysAndFormatsNumbers() {
        assertThat(Jcs.canonicalString(Json.parse("{\"b\": 1.0, \"a\": [true, null, \"\\u00e9\"], \"c\": 1E2}")))
                .isEqualTo("{\"a\":[true,null,\"\u00e9\"],\"b\":1,\"c\":100}");
        assertThat(Jcs.sdHash(Json.parse("{}"))).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void validSignatureVerifiesAndAnyChangeFails() {
        ObjectNode vc = TestVc.signed("provider-a", "provider-a", "t0123456789", "CONSUMER");
        SignatureVerifier.Result ok = SignatureVerifier.verify(vc);
        assertThat(ok.valid()).isTrue();
        assertThat(ok.publicX()).isEqualTo(TestVc.x("provider-a"));

        ObjectNode changed = vc.deepCopy();
        ((ObjectNode) changed.get("credentialSubject").get("simpl:generalServiceProperties")).put("simpl:name", "other");
        assertThat(SignatureVerifier.verify(changed).valid()).isFalse();

        ObjectNode otherIssuer = vc.deepCopy();
        otherIssuer.put("issuer", TestVc.did("provider-b"));
        assertThat(SignatureVerifier.verify(otherIssuer).valid()).isFalse();
    }

    @Test
    void keyOrderInTheSubmittedJsonDoesNotMatter() {
        ObjectNode vc = TestVc.signed("provider-a", "provider-a", "t0123456789", "CONSUMER");
        ObjectNode reordered = Json.object();
        reordered.set("proof", vc.get("proof"));
        reordered.set("credentialSubject", vc.get("credentialSubject"));
        vc.fields().forEachRemaining(e -> { if (!reordered.has(e.getKey())) reordered.set(e.getKey(), e.getValue()); });
        assertThat(SignatureVerifier.verify(reordered).valid()).isTrue();
    }
}
