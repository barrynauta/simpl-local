package eu.simpl.vocabularymanager.web;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The operations of the OpenAPI file, path by path. Literal paths are listed before templated ones, as OpenAPI
 * matching requires (so /vocabularies/searches is never read as a vocabulary name).
 */
public final class RouteTable {

    /**
     * One path with its declared methods.
     *
     * @param pattern the path as a regular expression
     * @param methods the declared methods
     */
    public record Route(Pattern pattern, Set<String> methods) {

        /**
         * Gives the Allow header value.
         *
         * @return the methods, comma separated
         */
        public String allow() {
            return String.join(", ", methods.stream().sorted().toList());
        }
    }

    private static final String SEGMENT = "[^/]+";

    private static final List<Route> ROUTES = List.of(
            route("/v1/health", "GET"),
            route("/v1/vocabularies", "GET", "POST"),
            route("/v1/vocabularies/searches", "POST"),
            route("/v1/external-vocabularies", "GET", "POST"),
            route("/v1/external-vocabulary/searches", "POST"),
            route("/v1/external-vocabularies/content", "POST"),
            route("/v1/vocabularies/" + SEGMENT, "GET", "PATCH"),
            route("/v1/vocabularies/" + SEGMENT + "/versions", "GET", "POST"),
            route("/v1/vocabularies/" + SEGMENT + "/versions/" + SEGMENT, "GET", "PATCH"),
            route("/v1/content/" + SEGMENT, "GET"),
            route("/v1/content/" + SEGMENT + "/versions/" + SEGMENT, "GET"),
            route("/v1/external-vocabularies/" + SEGMENT + "/content", "GET"),
            route("/v1/external-vocabularies/" + SEGMENT, "GET", "DELETE"));

    private RouteTable() {}

    private static Route route(String regex, String... methods) {
        return new Route(Pattern.compile("^" + regex + "$"), Set.of(methods));
    }

    /**
     * Finds the route of a path.
     *
     * @param path the request path, without query
     * @return the route, empty when the path is not part of the API
     */
    public static Optional<Route> find(String path) {
        return ROUTES.stream().filter(r -> r.pattern().matcher(path).matches()).findFirst();
    }
}
