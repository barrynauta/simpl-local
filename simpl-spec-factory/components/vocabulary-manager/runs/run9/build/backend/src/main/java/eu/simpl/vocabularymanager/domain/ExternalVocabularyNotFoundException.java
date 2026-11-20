package eu.simpl.vocabularymanager.domain;

/** No external vocabulary for a name or a namespace URI (D41, D66). */
public class ExternalVocabularyNotFoundException extends RuntimeException {

    private final String issueIn;
    private final String issueName;

    private ExternalVocabularyNotFoundException(String detail, String issueIn, String issueName) {
        super(detail);
        this.issueIn = issueIn;
        this.issueName = issueName;
    }

    /** Lookup by name in a path. */
    public static ExternalVocabularyNotFoundException byName(String name) {
        return new ExternalVocabularyNotFoundException(
                "External vocabulary with name '" + name + "' was not found.", "path", "name");
    }

    /** Lookup by namespace URI in a body; the URI is shown as sent. */
    public static ExternalVocabularyNotFoundException byNamespace(String uri) {
        return new ExternalVocabularyNotFoundException(
                "External vocabulary with namespaceUri '" + uri + "' was not found.", "body", "namespaceUri");
    }

    /** @return where the failing value was */
    public String issueIn() {
        return issueIn;
    }

    /** @return the name of the failing value */
    public String issueName() {
        return issueName;
    }
}
