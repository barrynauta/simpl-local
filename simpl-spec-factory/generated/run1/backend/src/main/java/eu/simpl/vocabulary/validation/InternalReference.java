package eu.simpl.vocabulary.validation;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * D18 reference convention: {@code {base}/{Name}/{n}#Term} points to version n of an internal vocabulary,
 * {@code {base}/{Name}#Term} points to its latest version.
 */
public record InternalReference(String name, Integer version, String term) {

    private static final Pattern REMAINDER = Pattern.compile("^/([^/#]+)(?:/(\\d+))?(?:[#/](.*))?$");

    /** Returns empty when the IRI is not under the base, or a reference (possibly with an invalid name). */
    public static Optional<InternalReference> parse(String iri, String base) {
        if (iri == null || !iri.startsWith(base + "/")) {
            return Optional.empty();
        }
        Matcher m = REMAINDER.matcher(iri.substring(base.length()));
        if (!m.matches()) {
            return Optional.of(new InternalReference(iri.substring(base.length() + 1), null, null));
        }
        Integer version = null;
        if (m.group(2) != null) {
            try {
                version = Integer.parseInt(m.group(2));
            } catch (NumberFormatException e) {
                version = Integer.MAX_VALUE;
            }
        }
        String term = m.group(3);
        if (term != null && term.isEmpty()) {
            term = null;
        }
        return Optional.of(new InternalReference(m.group(1), version, term));
    }

    public String namespace(String base) {
        return base + "/" + name + (version == null ? "" : "/" + version) + "#";
    }
}
