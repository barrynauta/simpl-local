package eu.simpl.schemamanager.validation;

/** The Vocabulary Manager did not answer, answered 5xx or timed out (D36). */
public class VocabularyUnavailableException extends RuntimeException {
    public VocabularyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
