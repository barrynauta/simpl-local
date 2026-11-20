package eu.simpl.vocabularymanager.domain;

/** The vocabulary exists but not this version (D41, D55). */
public class VersionNotFoundException extends RuntimeException {

    /**
     * @param name vocabulary name
     * @param version requested version
     */
    public VersionNotFoundException(String name, int version) {
        super("Version " + version + " of vocabulary '" + name + "' was not found.");
    }
}
