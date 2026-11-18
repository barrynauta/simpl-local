package eu.simpl.vocabularymanager.error;

/** An error code fixes the HTTP status and the RFC 9457 problem type, href and title of an error. */
public interface ErrorCode {

    int status();

    String type();

    /** Link to the problem type documentation, or null when there is none. */
    String href();

    String title();
}
