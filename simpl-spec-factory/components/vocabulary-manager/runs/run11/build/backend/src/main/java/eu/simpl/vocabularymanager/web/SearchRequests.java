package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.validation.Names;
import eu.simpl.vocabularymanager.validation.UriRules;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * Request bodies of the two searches (D38, D52, D68, D110, D112, D137) and of the content lookup
 * by namespace (D66, D73).
 */
public final class SearchRequests {

    static final int MAX_REFERENCES = 100;

    private SearchRequests() {}

    /**
     * The references of POST /vocabularies/searches, repeated references removed.
     *
     * @throws ApiException 400 with one issue per problem
     */
    public static List<VocabularyController.SearchReference> internal(JsonNode body) {
        List<Issue> issues = new ArrayList<>();
        unknown(body, Set.of("items"), "", issues);
        JsonNode items = body.get("items");
        List<VocabularyController.SearchReference> references = new ArrayList<>();
        if (items == null || !items.isArray()) {
            issues.add(Issue.schema("body", "items", "Property 'items' is required and must be an array."));
        } else if (items.isEmpty() || items.size() > MAX_REFERENCES) {
            issues.add(Issue.schema("body", "items", "Property 'items' must have 1 to 100 entries."));
        } else {
            for (int i = 0; i < items.size(); i++) {
                VocabularyController.SearchReference ref = reference(items.get(i), "items[" + i + "]", issues);
                if (ref != null && !references.contains(ref)) {
                    references.add(ref);
                }
            }
        }
        if (!issues.isEmpty()) {
            throw ApiException.badRequest(issues);
        }
        return references;
    }

    /**
     * The URIs of POST /external-vocabulary/searches in lookup order: namespaceUri or aliasUri, or
     * namespaceUris then aliasUris.
     *
     * @throws ApiException 400 with one issue per problem, name 'body' for the oneOf rule
     */
    public static List<String> external(JsonNode body) {
        List<Issue> issues = new ArrayList<>();
        unknown(body, Set.of("namespaceUri", "aliasUri", "namespaceUris", "aliasUris"), "", issues);
        if (!issues.isEmpty()) {
            throw ApiException.badRequest(issues);
        }
        boolean single = body.has("namespaceUri");
        boolean alias = body.has("aliasUri");
        boolean batch = body.has("namespaceUris") || body.has("aliasUris");
        int branches = (single ? 1 : 0) + (alias ? 1 : 0) + (batch ? 1 : 0);
        if (branches != 1) {
            throw ApiException.badRequest(
                    "body", "body", "Send exactly one of namespaceUri, aliasUri, or namespaceUris and aliasUris.");
        }
        List<String> uris = new ArrayList<>();
        if (single) {
            uris.add(uri(body.get("namespaceUri"), "namespaceUri", issues));
        } else if (alias) {
            uris.add(uri(body.get("aliasUri"), "aliasUri", issues));
        } else {
            uris.addAll(array(body, "namespaceUris", issues));
            uris.addAll(array(body, "aliasUris", issues));
            int total = size(body, "namespaceUris") + size(body, "aliasUris");
            if (total > MAX_REFERENCES) {
                String name = body.has("aliasUris") ? "aliasUris" : "namespaceUris";
                issues.add(Issue.schema("body", name, "At most 100 URIs in total."));
            }
        }
        if (!issues.isEmpty()) {
            throw ApiException.badRequest(issues);
        }
        return uris;
    }

    /**
     * The namespaceUri of POST /external-vocabularies/content.
     *
     * @throws ApiException 400
     */
    public static String contentNamespace(JsonNode body) {
        List<Issue> issues = new ArrayList<>();
        unknown(body, Set.of("namespaceUri"), "", issues);
        JsonNode value = body.get("namespaceUri");
        String uri = null;
        if (value == null) {
            issues.add(Issue.schema("body", "namespaceUri", "Property 'namespaceUri' is required."));
        } else {
            uri = uri(value, "namespaceUri", issues);
        }
        if (!issues.isEmpty()) {
            throw ApiException.badRequest(issues);
        }
        return uri;
    }

    private static VocabularyController.SearchReference reference(JsonNode item, String path, List<Issue> issues) {
        if (item == null || !item.isObject()) {
            issues.add(Issue.schema("body", path, "Each item must be an object with a name."));
            return null;
        }
        int before = issues.size();
        unknown(item, Set.of("name", "version"), path + ".", issues);
        JsonNode name = item.get("name");
        if (name == null || !name.isString() || !Names.isVocabularyName(name.asString())) {
            issues.add(Issue.schema(
                    "body", path + ".name", "Property 'name' must match ^[A-Z][a-zA-Z0-9]*$ (3 to 64 characters)."));
        }
        Integer version = null;
        JsonNode v = item.get("version");
        if (v != null) {
            if (!v.isIntegralNumber()
                    || v.bigIntegerValue().compareTo(BigInteger.ONE) < 0
                    || v.bigIntegerValue().compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                issues.add(Issue.schema(
                        "body", path + ".version", "Property 'version' must be an integer from 1 to 2147483647."));
            } else {
                version = v.intValue();
            }
        }
        if (issues.size() > before) {
            return null;
        }
        return new VocabularyController.SearchReference(name.asString(), version);
    }

    private static List<String> array(JsonNode body, String field, List<Issue> issues) {
        JsonNode node = body.get(field);
        List<String> values = new ArrayList<>();
        if (node == null) {
            return values;
        }
        if (!node.isArray() || node.isEmpty()) {
            issues.add(Issue.schema("body", field, "Property '" + field + "' must be an array with at least one URI."));
            return values;
        }
        for (int i = 0; i < node.size(); i++) {
            String value = uri(node.get(i), field + "[" + i + "]", issues);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private static int size(JsonNode body, String field) {
        JsonNode node = body.get(field);
        return node != null && node.isArray() ? node.size() : 0;
    }

    private static String uri(JsonNode value, String name, List<Issue> issues) {
        if (value == null || !value.isString() || !UriRules.isAbsoluteUri(value.asString())) {
            issues.add(Issue.schema("body", name, "'" + name + "' must be an absolute URI."));
            return null;
        }
        return value.asString();
    }

    private static void unknown(JsonNode object, Set<String> allowed, String prefix, List<Issue> issues) {
        Set<String> unknown = new LinkedHashSet<>(JsonBody.unknownProperties(object, allowed));
        for (String name : unknown) {
            issues.add(Issue.schema("body", prefix + name, "Property '" + prefix + name + "' is not supported."));
        }
    }
}
