package eu.simpl.vocabularymanager.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One Belgif issue of a problem response.
 *
 * @param type issue type, or null
 * @param href issue documentation, or null
 * @param title issue title, or null
 * @param detail detail text
 * @param in body, header, path or query
 * @param name name of the failing input
 * @param value the failing value, or null
 * @param location location in uploaded content, or null
 */
public record Issue(
        String type, String href, String title, String detail, String in, String name, Object value, String location) {

    private static final String GUIDE = "https://www.belgif.be/specification/rest/api-guide/issues/";

    /** A request value that breaks the schema of the decided contract. */
    public static Issue schemaViolation(String in, String name, String detail, Object value) {
        return new Issue(
                "urn:problem-type:belgif:input-validation:schemaViolation",
                GUIDE + "schemaViolation.html",
                "Input isn't valid with respect to schema",
                detail,
                in,
                name,
                value,
                null);
    }

    /** Invalid vocabulary content (D10). */
    public static Issue invalidInput(String name, String detail, String location) {
        return new Issue(
                "urn:problem-type:belgif:input-validation:invalidInput",
                GUIDE + "invalidInput.html",
                "Invalid input",
                detail,
                "body",
                name,
                null,
                location);
    }

    /** A not-found issue as in the OpenAPI example. */
    public static Issue notFound(String in, String name, String detail, Object value) {
        return new Issue(null, null, null, detail, in, name, value, null);
    }

    /** Wire form; absent values are left out. */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        put(map, "type", type);
        put(map, "href", href);
        put(map, "title", title);
        put(map, "detail", detail);
        put(map, "in", in);
        put(map, "name", name);
        put(map, "value", value);
        put(map, "location", location);
        return map;
    }

    private static void put(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
