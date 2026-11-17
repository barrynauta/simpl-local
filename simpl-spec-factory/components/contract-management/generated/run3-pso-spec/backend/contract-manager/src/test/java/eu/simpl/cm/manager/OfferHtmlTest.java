package eu.simpl.cm.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.cm.manager.service.OfferHtml;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** D21: content, order, escaping and SHA-256 hash. */
class OfferHtmlTest {

    @Test
    void buildsEscapedHtmlInOrderWithItsHash() throws Exception {
        var asset = new ObjectMapper().readTree("""
                {"assetId":"asset-1","resourceDescriptionId":"rd-1","resourceName":"Air <quality>",
                 "description":"d & e","policies":[{"type":"USAGE","description":"Research"}],
                 "contractClauses":[{"documentName":"terms.pdf","hash":"h1"}]}""");
        String html = OfferHtml.build(asset, "Provider One", "Consumer One");
        assertThat(html).doesNotStartWith("<html").contains("Air &lt;quality&gt;").contains("d &amp; e")
                .contains("rd-1").contains("asset-1").contains("Research").contains("terms.pdf").contains("h1");
        assertThat(html.indexOf("rd-1")).isLessThan(html.indexOf("asset-1"));
        assertThat(html.indexOf("Provider One")).isLessThan(html.indexOf("Consumer One"));
        assertThat(OfferHtml.sha256Hex("<p>x</p>"))
                .isEqualTo("31d8e07ec305ac4e2515d1f0b1c8c603b3858044eb7d97807d6d0970838cb0be");
    }
}
