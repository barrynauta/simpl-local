package eu.simpl.cm.common;

import java.util.regex.Pattern;

/** Id formats of D16. */
public final class Ids {

    public static final Pattern ID = Pattern.compile("^[A-Za-z0-9:-]{1,100}$");
    public static final Pattern UUID = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private Ids() {
    }

    public static boolean isId(String s) {
        return s != null && ID.matcher(s).matches();
    }

    public static boolean isUuid(String s) {
        return s != null && UUID.matcher(s).matches();
    }

    /** Path parameter check (D84 step 2). */
    public static void requirePathId(String name, String value) {
        if (!isId(value)) {
            throw ApiException.argumentTypeMismatch("Invalid path parameter " + name);
        }
    }

    public static void requirePathUuid(String name, String value) {
        if (!isUuid(value)) {
            throw ApiException.argumentTypeMismatch("Invalid path parameter " + name + ": must be a UUID");
        }
    }
}
