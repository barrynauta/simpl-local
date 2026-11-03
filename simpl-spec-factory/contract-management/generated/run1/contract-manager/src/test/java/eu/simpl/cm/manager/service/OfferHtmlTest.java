package eu.simpl.cm.manager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.cm.common.proof.DataIntegrity;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class OfferHtmlTest {
    @Test
    void containsValuesInOrderEscapedAndHashed() throws Exception {
        var asset = new ObjectMapper().readTree("""
                {"assetId":"asset-1","resourceDescriptionId":"rd-1","resourceName":"Air <quality>","description":"d & e",
                 "policies":[{"type":"USAGE","description":"No resale"}],
                 "contractClauses":[{"documentName":"clauses.pdf","hash":"abc"}]}""");
        String html = OfferHtml.build(asset, "Provider One", "Consumer One");
        assertThat(html).doesNotContain("<html").contains("Air &lt;quality&gt;").contains("d &amp; e")
                .contains("No resale").contains("clauses.pdf").contains("abc");
        assertThat(html.indexOf("rd-1")).isLessThan(html.indexOf("asset-1"));
        assertThat(html.indexOf("Provider One")).isLessThan(html.indexOf("Consumer One"));
        assertThat(html.indexOf("Consumer One")).isLessThan(html.indexOf("No resale"));
        assertThat(OfferHtml.hash(html)).isEqualTo(DataIntegrity.sha256Hex(html.getBytes(StandardCharsets.UTF_8)))
                .matches("[0-9a-f]{64}");
    }

    @Test
    void walletErrorsD57() {
        assertThat(SigningService.walletError(503)).isEqualTo(new SigningService.WalletError("WALLET_UNAVAILABLE", "Secure storage location unavailable"));
        assertThat(SigningService.walletError(507)).isEqualTo(new SigningService.WalletError("WALLET_FULL", "Secure storage location is full"));
        assertThat(SigningService.walletError(500)).isEqualTo(new SigningService.WalletError("WALLET_CORRUPTED", "Secure storage location is corrupted"));
        assertThat(SigningService.walletError(-1).code()).isEqualTo("WALLET_UNAVAILABLE");
        assertThat(SigningService.walletError(418).code()).isEqualTo("WALLET_UNAVAILABLE");
    }

    @Test
    void reasonRules() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> LifecycleService.checkReason(
                eu.simpl.cm.manager.domain.LifecycleAction.SUSPEND, "   ")).hasMessageContaining("reason");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> LifecycleService.checkReason(
                eu.simpl.cm.manager.domain.LifecycleAction.REACTIVATE, "x".repeat(1001))).hasMessageContaining("1000");
        assertThat(LifecycleService.checkReason(eu.simpl.cm.manager.domain.LifecycleAction.REACTIVATE, null)).isNull();
    }
}
