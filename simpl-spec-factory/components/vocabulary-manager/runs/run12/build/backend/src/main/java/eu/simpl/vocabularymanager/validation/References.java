package eu.simpl.vocabularymanager.validation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Classifies the namespace declarations of an upload into internal and external vocabulary
 * references (decisions D12, D69, D145, D146). Pure logic, no store access.
 */
public final class References {

    public static final Pattern NAME = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");

    /** W3C namespaces that never need a registered copy (D12), normalised. */
    public static final Set<String> W3C = Set.of(
            "http://www.w3.org/1999/02/22-rdf-syntax-ns",
            "http://www.w3.org/2000/01/rdf-schema",
            "http://www.w3.org/2002/07/owl",
            "http://www.w3.org/2001/XMLSchema",
            "http://www.w3.org/2004/02/skos/core");

    /**
     * One reference.
     *
     * @param prefix prefix label of the first declaration
     * @param namespace the namespace IRI
     * @param position position of the first declaration
     * @param internalName vocabulary name for an internal reference, else null
     * @param versionText version as written for a versioned internal reference, else null (latest)
     */
    public record Ref(String prefix, String namespace, int position, String internalName, String versionText) {

        public boolean internal() {
            return internalName != null;
        }

        /** prefix 'p' -&gt; 'namespace' (D65). */
        public String location() {
            return "prefix '" + prefix + "' -> '" + namespace + "'";
        }

        /** The version number when the version text is a positive 32-bit integer, else -1. */
        public int versionNumber() {
            if (versionText == null || versionText.isEmpty() || versionText.length() > 10) {
                return -1;
            }
            for (char c : versionText.toCharArray()) {
                if (c < '0' || c > '9') {
                    return -1;
                }
            }
            long v = Long.parseLong(versionText);
            return v >= 1 && v <= Integer.MAX_VALUE ? (int) v : -1;
        }
    }

    private References() {}

    /** Removes one trailing '#' or '/' (D30). */
    public static String normalise(String uri) {
        if (uri.endsWith("#") || uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    /** Each distinct namespace once, with its first declaration (D145). */
    public static List<Ref> classify(List<ParsedTurtle.Prefix> prefixes, String contentBase) {
        Map<String, Ref> seen = new LinkedHashMap<>();
        String internalBase = contentBase + "/content/";
        for (ParsedTurtle.Prefix p : prefixes) {
            if (seen.containsKey(p.namespace())) {
                continue;
            }
            seen.put(p.namespace(), classifyOne(p, internalBase));
        }
        return new ArrayList<>(seen.values());
    }

    private static Ref classifyOne(ParsedTurtle.Prefix p, String internalBase) {
        String ns = p.namespace();
        if (ns.startsWith(internalBase)) {
            String rest = ns.substring(internalBase.length());
            int end = 0;
            while (end < rest.length() && rest.charAt(end) != '/' && rest.charAt(end) != '#') {
                end++;
            }
            String name = rest.substring(0, end);
            if (name.length() >= 3 && name.length() <= 64 && NAME.matcher(name).matches()) {
                String tail = rest.substring(end);
                if (tail.startsWith("/versions/")) {
                    String v = tail.substring("/versions/".length());
                    int vend = 0;
                    while (vend < v.length() && v.charAt(vend) != '/' && v.charAt(vend) != '#') {
                        vend++;
                    }
                    return new Ref(p.prefix(), ns, p.position(), name, v.substring(0, vend));
                }
                return new Ref(p.prefix(), ns, p.position(), name, null);
            }
        }
        return new Ref(p.prefix(), ns, p.position(), null, null);
    }
}
