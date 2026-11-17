package eu.simpl.catalogue.cat.api;

import eu.simpl.catalogue.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

/** Query parameter parsing helpers. */
final class Params {
    private Params() {}

    /** Comma separated (explode false) or repeated values; null when absent. */
    static List<String> list(HttpServletRequest req, String name) {
        String[] values = req.getParameterValues(name);
        if (values == null) return null;
        List<String> out = new ArrayList<>();
        for (String v : values) {
            for (String p : v.split(",")) if (!p.isBlank()) out.add(p.trim());
        }
        return out;
    }

    static boolean bool(String v, String name, boolean def) {
        if (v == null || v.isEmpty()) return def;
        if (v.equalsIgnoreCase("true")) return true;
        if (v.equalsIgnoreCase("false")) return false;
        throw ApiException.invalidInput("Parameter '" + name + "' must be true or false.");
    }

    static int integer(String v, String name, int def, int min, int max) {
        if (v == null || v.isEmpty()) return def;
        try {
            int i = Integer.parseInt(v.trim());
            if (i < min || i > max) throw new NumberFormatException();
            return i;
        } catch (NumberFormatException e) {
            throw ApiException.invalidInput("Parameter '" + name + "' must be an integer from " + min + " to " + max + ".");
        }
    }
}
