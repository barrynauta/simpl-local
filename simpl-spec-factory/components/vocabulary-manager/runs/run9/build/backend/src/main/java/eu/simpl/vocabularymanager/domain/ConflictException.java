package eu.simpl.vocabularymanager.domain;

/** A conflict with the stored state: duplicate name, same status, identical version (D4, D15, D46, D30). */
public class ConflictException extends RuntimeException {

    /** @param detail decided detail text */
    public ConflictException(String detail) {
        super(detail);
    }
}
