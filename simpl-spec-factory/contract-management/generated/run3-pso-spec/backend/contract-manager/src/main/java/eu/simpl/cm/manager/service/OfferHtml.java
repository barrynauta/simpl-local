package eu.simpl.cm.manager.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Human-readable offer and its hash (D21). */
public final class OfferHtml {

    private OfferHtml() {
    }

    private static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s, "UTF-8");
    }

    public static String build(JsonNode asset, String providerName, String consumerName) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"usage-contract-offer\">");
        sb.append("<h2>").append(esc(Directory.text(asset, "resourceName"))).append("</h2>");
        sb.append("<table>");
        row(sb, "Resource name", Directory.text(asset, "resourceName"));
        row(sb, "Resource description ID", Directory.text(asset, "resourceDescriptionId"));
        row(sb, "Asset ID", Directory.text(asset, "assetId"));
        row(sb, "Provider", providerName);
        row(sb, "Consumer", consumerName);
        row(sb, "Description", Directory.text(asset, "description"));
        sb.append("</table>");
        sb.append("<h3>Policies</h3><ul class=\"policies\">");
        JsonNode policies = asset.path("policies");
        if (policies.isArray()) {
            for (JsonNode p : policies) {
                sb.append("<li><strong>").append(esc(Directory.text(p, "type"))).append("</strong>: ")
                        .append(esc(Directory.text(p, "description"))).append("</li>");
            }
        }
        sb.append("</ul>");
        sb.append("<h3>Contract clauses</h3><ul class=\"contract-clauses\">");
        JsonNode clauses = asset.path("contractClauses");
        if (clauses.isArray()) {
            for (JsonNode c : clauses) {
                sb.append("<li>").append(esc(Directory.text(c, "documentName"))).append(" (SHA-256: ")
                        .append(esc(Directory.text(c, "hash"))).append(")</li>");
            }
        }
        sb.append("</ul></div>");
        return sb.toString();
    }

    private static void row(StringBuilder sb, String label, String value) {
        sb.append("<tr><th>").append(esc(label)).append("</th><td>").append(esc(value)).append("</td></tr>");
    }

    public static String sha256Hex(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
