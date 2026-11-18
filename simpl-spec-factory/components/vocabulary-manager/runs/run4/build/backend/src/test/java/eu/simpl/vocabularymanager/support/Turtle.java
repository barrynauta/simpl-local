package eu.simpl.vocabularymanager.support;

import java.nio.charset.StandardCharsets;

/** Turtle test content. */
public final class Turtle {

    public static final String BASE = "https://authority.example/vocabulary-manager";

    private Turtle() {}

    public static byte[] simple(String label) {
        return ("@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                        + "@prefix ex: <https://example.eu/test#> .\n"
                        + "ex:Scheme a skos:ConceptScheme ;\n"
                        + "  skos:prefLabel \"" + label + "\"@en .\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] referencing(String prefix, String namespace) {
        return ("@prefix " + prefix + ": <" + namespace + "> .\n"
                        + "@prefix ex: <https://example.eu/ref#> .\n"
                        + "ex:Thing a ex:Class .\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
