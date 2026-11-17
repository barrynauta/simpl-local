package eu.simpl.cm.manager.service;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.cm.common.proof.DataIntegrity;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

/** D21: the human-readable offer and its SHA-256 hash. */
public final class OfferHtml {
    private OfferHtml() {
    }

    public static String build(JsonNode asset, String providerName, String consumerName) {
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"usage-contract-offer\">");
        sb.append("<h2>").append(esc(text(asset, "resourceName"))).append("</h2>");
        sb.append("<table>");
        row(sb, "Resource name", text(asset, "resourceName"));
        row(sb, "Resource description id", text(asset, "resourceDescriptionId"));
        row(sb, "Asset id", text(asset, "assetId"));
        row(sb, "Provider", providerName);
        row(sb, "Consumer", consumerName);
        row(sb, "Description", text(asset, "description"));
        sb.append("</table>");
        sb.append("<h3>Policies</h3><ul class=\"policies\">");
        for (JsonNode p : asset.path("policies")) {
            sb.append("<li>").append(esc(text(p, "type"))).append(": ").append(esc(text(p, "description"))).append("</li>");
        }
        sb.append("</ul>");
        sb.append("<h3>Contract clauses</h3><ul class=\"contract-clauses\">");
        for (JsonNode c : asset.path("contractClauses")) {
            sb.append("<li>").append(esc(text(c, "documentName"))).append(" (SHA-256 ").append(esc(text(c, "hash")))
                    .append(")</li>");
        }
        sb.append("</ul></section>");
        return sb.toString();
    }

    public static String hash(String html) {
        return DataIntegrity.sha256Hex(html.getBytes(StandardCharsets.UTF_8));
    }

    private static void row(StringBuilder sb, String label, String value) {
        sb.append("<tr><th>").append(esc(label)).append("</th><td>").append(esc(value)).append("</td></tr>");
    }

    private static String text(JsonNode n, String f) {
        JsonNode v = n == null ? null : n.get(f);
        return v == null || v.isNull() ? "" : v.asText();
    }

    private static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s, "UTF-8");
    }
}
