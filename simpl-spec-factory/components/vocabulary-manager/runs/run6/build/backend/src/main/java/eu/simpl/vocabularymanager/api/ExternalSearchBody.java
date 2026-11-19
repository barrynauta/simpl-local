package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.problem.Issue;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * ExternalVocabularySearchRequest (decision D38): exactly one oneOf branch must match, either namespaceUri, or
 * aliasUri, or a batch of namespaceUris and/or aliasUris (each array at least 1 entry, at most 100 URIs in total).
 */
final class ExternalSearchBody {

    private static final Set<String> PROPERTIES = Set.of("namespaceUri", "aliasUri", "namespaceUris", "aliasUris");

    private ExternalSearchBody() {}

    /**
     * Reads the URIs to search, namespace URIs before alias URIs.
     *
     * @param body the JSON body
     * @param maxUris the most URIs in total
     * @param issues the issue list
     * @return the URIs in search order
     */
    static List<String> read(JsonNode body, int maxUris, List<Issue> issues) {
        if (!JsonBodyReader.checkObject(body, "", PROPERTIES, issues)) {
            return List.of();
        }
        boolean single = body.has("namespaceUri");
        boolean alias = body.has("aliasUri");
        boolean batch = body.has("namespaceUris") || body.has("aliasUris");
        int branches = (single ? 1 : 0) + (alias ? 1 : 0) + (batch ? 1 : 0);
        if (branches != 1) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_BODY,
                    "body",
                    "Give exactly one of namespaceUri, aliasUri, or namespaceUris and/or aliasUris."));
            return List.of();
        }
        List<String> uris = new ArrayList<>();
        if (single) {
            readUri(body.get("namespaceUri"), "namespaceUri", uris, issues);
        } else if (alias) {
            readUri(body.get("aliasUri"), "aliasUri", uris, issues);
        } else {
            readArray(body.get("namespaceUris"), "namespaceUris", uris, issues);
            readArray(body.get("aliasUris"), "aliasUris", uris, issues);
            if (uris.size() > maxUris) {
                String name = body.has("aliasUris") ? "aliasUris" : "namespaceUris";
                issues.add(Issue.schemaViolation(
                        Issue.IN_BODY, name, "At most " + maxUris + " URIs in total are allowed."));
            }
        }
        return uris;
    }

    private static void readUri(JsonNode node, String name, List<String> uris, List<Issue> issues) {
        if (node == null || !node.isString() || !UriValues.isAbsoluteUri(node.asString())) {
            issues.add(Issue.schemaViolation(Issue.IN_BODY, name, "Field '" + name + "' must be an absolute URI."));
            return;
        }
        uris.add(node.asString());
    }

    private static void readArray(JsonNode node, String name, List<String> uris, List<Issue> issues) {
        if (node == null) {
            return;
        }
        if (!node.isArray() || node.isEmpty()) {
            issues.add(Issue.schemaViolation(
                    Issue.IN_BODY, name, "Field '" + name + "' must be an array with at least 1 URI."));
            return;
        }
        for (JsonNode item : node) {
            if (!item.isString() || !UriValues.isAbsoluteUri(item.asString())) {
                issues.add(Issue.schemaViolation(
                        Issue.IN_BODY, name, "Every entry of '" + name + "' must be an absolute URI."));
                return;
            }
            uris.add(item.asString());
        }
    }
}
