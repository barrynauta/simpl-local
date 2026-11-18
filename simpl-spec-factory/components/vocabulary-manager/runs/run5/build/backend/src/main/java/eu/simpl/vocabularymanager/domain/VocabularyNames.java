package eu.simpl.vocabularymanager.domain;

import java.util.regex.Pattern;

/** Rules for vocabulary names and version numbers (D37, D49). */
public final class VocabularyNames {

    private static final Pattern NAME = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");
    private static final Pattern VERSION = Pattern.compile("^[1-9][0-9]{0,8}$");

    private VocabularyNames() {}

    /** True when the name has 3 to 64 characters and matches ^[A-Z][a-zA-Z0-9]*$. */
    public static boolean isValidName(String name) {
        return name != null
                && name.length() >= 3
                && name.length() <= 64
                && NAME.matcher(name).matches();
    }

    /** True when the text is a positive integer that fits an int. */
    public static boolean isValidVersion(String text) {
        return text != null && VERSION.matcher(text).matches();
    }
}
