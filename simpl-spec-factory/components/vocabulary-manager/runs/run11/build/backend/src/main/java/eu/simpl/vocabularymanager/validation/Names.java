package eu.simpl.vocabularymanager.validation;

import java.util.regex.Pattern;

/** Field rules of D37, D19, D49, D56 and D112. */
public final class Names {

    public static final int NAME_MIN = 3;
    public static final int NAME_MAX = 64;
    public static final int TEXT_MAX = 1000;

    private static final Pattern NAME = Pattern.compile("[A-Z][a-zA-Z0-9]*");
    private static final Pattern NAME_FILTER = Pattern.compile("[A-Za-z0-9]{1,64}");
    private static final Pattern DIGITS = Pattern.compile("[0-9]+");

    private Names() {}

    /** Vocabulary and external vocabulary names: 3 to 64 characters, ^[A-Z][a-zA-Z0-9]*$. */
    public static boolean isVocabularyName(String value) {
        return value != null
                && value.length() >= NAME_MIN
                && value.length() <= NAME_MAX
                && NAME.matcher(value).matches();
    }

    /** The relaxed list name filter (D19). */
    public static boolean isNameFilter(String value) {
        return value != null && NAME_FILTER.matcher(value).matches();
    }

    /**
     * An integer in a path or query: digits only, 1 to max (D91, D112). Returns -1 when invalid.
     */
    public static long positiveInt(String value, long max) {
        if (value == null || value.length() > 12 || !DIGITS.matcher(value).matches()) {
            return -1;
        }
        long parsed = Long.parseLong(value);
        return parsed >= 1 && parsed <= max ? parsed : -1;
    }

    /** Number of characters (code points) after trimming. */
    public static int trimmedLength(String value) {
        String trimmed = value.strip();
        return trimmed.codePointCount(0, trimmed.length());
    }
}
