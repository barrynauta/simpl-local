package eu.simpl.vocabularymanager.validation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The vocabulary references of an upload, classified as D12 and D69 say.
 *
 * @param internal references to internal vocabularies or versions
 * @param external namespaces that need a registered external vocabulary
 */
public record ReferenceAnalysis(List<InternalReference> internal, List<ExternalReference> external) {

    /** Names of the referenced internal vocabularies. */
    public Set<String> internalNames() {
        return internal.stream().map(InternalReference::name).collect(Collectors.toCollection(java.util.TreeSet::new));
    }

    /** Comparison keys of the external namespaces. */
    public Set<String> externalKeys() {
        return external.stream().map(ExternalReference::key).collect(Collectors.toCollection(java.util.TreeSet::new));
    }

    /** Whether no store lookup is needed. */
    public boolean isEmpty() {
        return internal.isEmpty() && external.isEmpty();
    }

    /**
     * Reference to an internal vocabulary.
     *
     * @param declaration the prefix declaration
     * @param name vocabulary name
     * @param versionText version as written, or null for the latest version
     */
    public record InternalReference(PrefixDeclaration declaration, String name, String versionText) {

        /** The version number, or null when absent or not a positive 32-bit integer. */
        public Integer versionNumber() {
            if (versionText == null || !versionText.matches("[1-9][0-9]{0,9}")) {
                return null;
            }
            long value = Long.parseLong(versionText);
            return value > Integer.MAX_VALUE ? null : (int) value;
        }
    }

    /**
     * Reference to an external namespace.
     *
     * @param declaration the prefix declaration
     * @param key comparison key of the namespace (D30)
     */
    public record ExternalReference(PrefixDeclaration declaration, String key) {}
}
