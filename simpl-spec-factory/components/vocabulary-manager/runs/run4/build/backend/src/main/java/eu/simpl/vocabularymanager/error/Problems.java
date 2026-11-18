package eu.simpl.vocabularymanager.error;

import java.util.List;

/** Factory methods for the exceptions the component raises, so texts live in one place. */
public final class Problems {

    static final String BELGIF_PROBLEMS = "https://www.belgif.be/specification/rest/api-guide/problems/";
    public static final String INPUT_INCORRECT = "The input message is incorrect";
    public static final String STORE_UNAVAILABLE = "The vocabulary store is not available. Retry later.";
    public static final String IDENTITY_PROVIDER_UNAVAILABLE = "The identity provider is not available. Retry later.";
    public static final String UPLOAD_TOO_LARGE =
            "Uploaded vocabulary file is too large. The maximum allowed upload size"
                    + " is 5 MB. Please upload a smaller Turtle file.";

    private Problems() {}

    public static VocabularyManagerException badRequest(List<Issue> issues) {
        return new VocabularyManagerException(RequestErrorCode.BAD_REQUEST, INPUT_INCORRECT, issues);
    }

    public static VocabularyManagerException badRequest(Issue issue) {
        return badRequest(List.of(issue));
    }

    public static VocabularyManagerException vocabularyNotFound(String name) {
        String detail = "Vocabulary with name '" + name + "' was not found.";
        return new VocabularyManagerException(
                ResourceErrorCode.NOT_FOUND, detail, List.of(Issue.at("path", "vocabularyName", name, detail)));
    }

    public static VocabularyManagerException versionNotFound(String name, int version) {
        String detail = "Version " + version + " of vocabulary '" + name + "' was not found.";
        return new VocabularyManagerException(
                ResourceErrorCode.NOT_FOUND, detail, List.of(Issue.at("path", "version", version, detail)));
    }

    public static VocabularyManagerException externalNotFound(String name) {
        String detail = "External vocabulary with name '" + name + "' was not found.";
        return new VocabularyManagerException(
                ResourceErrorCode.NOT_FOUND, detail, List.of(Issue.at("path", "name", name, detail)));
    }

    public static VocabularyManagerException externalNamespaceNotFound(String namespaceUri) {
        String detail = "External vocabulary with namespaceUri '" + namespaceUri + "' was not found.";
        return new VocabularyManagerException(
                ResourceErrorCode.NOT_FOUND, detail, List.of(Issue.at("body", "namespaceUri", namespaceUri, detail)));
    }

    public static VocabularyManagerException conflict(String detail) {
        return new VocabularyManagerException(ResourceErrorCode.CONFLICT, detail);
    }

    public static VocabularyManagerException uploadTooLarge() {
        return new VocabularyManagerException(
                RequestErrorCode.PAYLOAD_TOO_LARGE,
                UPLOAD_TOO_LARGE,
                List.of(Issue.invalidInput("vocabularyFile", UPLOAD_TOO_LARGE, null)));
    }

    public static VocabularyManagerException storeUnavailable(Throwable cause) {
        return new VocabularyManagerException(
                TechnicalErrorCode.SERVICE_UNAVAILABLE, STORE_UNAVAILABLE, List.of(), cause);
    }

    public static VocabularyManagerException identityProviderUnavailable(Throwable cause) {
        return new VocabularyManagerException(
                TechnicalErrorCode.SERVICE_UNAVAILABLE, IDENTITY_PROVIDER_UNAVAILABLE, List.of(), cause);
    }

    public static VocabularyManagerException storeError(String detail, Throwable cause) {
        return new VocabularyManagerException(TechnicalErrorCode.INTERNAL, detail, List.of(), cause);
    }
}
