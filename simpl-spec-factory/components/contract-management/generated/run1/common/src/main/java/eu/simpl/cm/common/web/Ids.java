package eu.simpl.cm.common.web;

import java.util.UUID;
import java.util.regex.Pattern;

/** D16 identifier formats. */
public final class Ids {
    private static final Pattern ID = Pattern.compile("^[A-Za-z0-9:-]{1,100}$");

    private Ids() {
    }

    public static boolean isId(String s) {
        return s != null && ID.matcher(s).matches();
    }

    public static boolean isUuid(String s) {
        if (s == null || s.length() != 36) {
            return false;
        }
        try {
            UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Path id check: 400 argumentTypeMismatch. */
    public static String pathId(String name, String value) {
        if (!isId(value)) {
            throw ApiException.typeMismatch("Invalid " + name);
        }
        return value;
    }

    public static String pathUuid(String name, String value) {
        if (!isUuid(value)) {
            throw ApiException.typeMismatch("Invalid " + name + ", a UUID is expected");
        }
        return value;
    }
}
