package eu.simpl.vocabularymanager.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/** Rules of the vocabulary name (D37, glossary). */
public final class VocabularyNames {

    public static final Pattern PATTERN = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");
    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 64;

    private VocabularyNames() {}

    /** True when the name follows D37. */
    public static boolean isValid(String name) {
        return name != null
                && name.length() >= MIN_LENGTH
                && name.length() <= MAX_LENGTH
                && PATTERN.matcher(name).matches();
    }

    /** The key for case-insensitive uniqueness (D37). */
    public static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
