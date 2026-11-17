package eu.simpl.schemamanager.web;

import eu.simpl.schemamanager.domain.SchemaStatus;
import eu.simpl.schemamanager.domain.SchemaType;
import java.util.regex.Pattern;

/** Parameter validation rules of D12, D13, D26 and D27. All failures give 400. */
public final class Params {

    public static final Pattern PASCAL = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");
    public static final Pattern VERSION = Pattern.compile("^[1-9][0-9]{0,8}$");
    public static final int MAX_PAGE_SIZE = 100;

    private Params() {
    }

    public static boolean isPascal(String value) {
        return value != null && value.length() >= 3 && value.length() <= 64 && PASCAL.matcher(value).matches();
    }

    public static void pathName(String name) {
        if (!isPascal(name)) {
            throw ApiException.badRequest(
                    "Path parameter schemaName must contain only alphanumeric characters and follow PascalCase (3 to 64 characters).");
        }
    }

    public static SchemaType schemaType(String value, String where) {
        if (value == null) {
            return SchemaType.RESOURCE;
        }
        return switch (value) {
            case "RESOURCE" -> SchemaType.RESOURCE;
            case "SEARCH" -> SchemaType.SEARCH;
            default -> throw ApiException.badRequest(
                    where + " 'schemaType' need to have value RESOURCE or SEARCH.");
        };
    }

    public static SchemaStatus status(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "PUBLISHED" -> SchemaStatus.PUBLISHED;
            case "REVOKED" -> SchemaStatus.REVOKED;
            default -> throw ApiException.badRequest("Query parameter 'status' need to have value REVOKED or PUBLISHED.");
        };
    }

    /**
     * Validates an optional resourceType query or form value. Returns null when absent.
     * For SEARCH any value is rejected (D13, D27).
     */
    public static String resourceTypeFilter(String value, SchemaType type, String where) {
        if (value == null) {
            return null;
        }
        if (type == SchemaType.SEARCH) {
            throw ApiException.badRequest(where + " 'resourceType' is not allowed when 'schemaType' is SEARCH.");
        }
        if (!isPascal(value)) {
            throw ApiException.badRequest(where
                    + " 'resourceType' must contain only alphanumeric characters and follow PascalCase (3 to 64 characters).");
        }
        return value;
    }

    public static int page(String value) {
        return intParam(value, "page", 1, 1, Integer.MAX_VALUE);
    }

    public static int pageSize(String value) {
        return intParam(value, "pageSize", 20, 1, MAX_PAGE_SIZE);
    }

    private static int intParam(String value, String name, int dflt, int min, int max) {
        if (value == null) {
            return dflt;
        }
        try {
            int v = Integer.parseInt(value.trim());
            if (v < min || v > max) {
                throw new NumberFormatException();
            }
            return v;
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Query parameter '" + name + "' must be an integer between " + min
                    + " and " + max + ".");
        }
    }

    /** A version path segment that is not a positive integer string gives 404 (D10). */
    public static Integer version(String value) {
        if (value == null || !VERSION.matcher(value).matches()) {
            return null;
        }
        return Integer.parseInt(value);
    }
}
