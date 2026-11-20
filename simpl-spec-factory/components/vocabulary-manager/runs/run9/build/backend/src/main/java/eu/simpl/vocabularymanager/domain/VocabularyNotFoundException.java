package eu.simpl.vocabularymanager.domain;

/** No vocabulary with this exact name is stored (D24, D54, D66). */
public class VocabularyNotFoundException extends RuntimeException {

    private final String name;

    /** @param name the requested name */
    public VocabularyNotFoundException(String name) {
        super("Vocabulary with name '" + name + "' was not found.");
        this.name = name;
    }

    /** @return the requested name */
    public String name() {
        return name;
    }
}
