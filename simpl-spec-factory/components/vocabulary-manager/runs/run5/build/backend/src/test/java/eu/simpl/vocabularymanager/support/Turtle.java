package eu.simpl.vocabularymanager.support;

import java.nio.charset.StandardCharsets;

/** Sample Turtle content. */
public final class Turtle {

    public static final String SKOS = "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n";

    private Turtle() {}

    public static byte[] valid(String name) {
        return (SKOS
                        + "@prefix ex: <https://example.eu/" + name + "#> .\n"
                        + "ex:Scheme a skos:ConceptScheme ; skos:prefLabel \"" + name + "\"@en .\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
