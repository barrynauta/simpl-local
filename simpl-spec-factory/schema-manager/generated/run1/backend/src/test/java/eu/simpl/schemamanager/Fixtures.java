package eu.simpl.schemamanager;

import java.nio.charset.StandardCharsets;

/** Turtle fixtures shared by the tests (D61 shape). */
public final class Fixtures {

    public static final String PREFIXES = """
            @prefix sh: <http://www.w3.org/ns/shacl#> .
            @prefix simpl: <https://simpl-programme.ec.europa.eu/ns/default#> .
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
            """;

    public static final String VALID = PREFIXES + """
            <urn:shape:Test> a sh:NodeShape ;
              sh:property [ sh:path simpl:uuid ; sh:datatype xsd:string ] ;
              sh:property [ sh:path simpl:version ; sh:datatype xsd:string ] .
            """;

    public static final String NO_VERSION = PREFIXES + """
            <urn:shape:Test> a sh:NodeShape ;
              sh:property [ sh:path simpl:uuid ; sh:datatype xsd:string ] .
            """;

    public static final String NO_SHAPE = PREFIXES + """
            <urn:x:a> <urn:x:b> "c" .
            """;

    public static final String BROKEN = PREFIXES + """
            <urn:shape:Test> a sh:NodeShape ;
              sh:property [ sh:path simpl:uuid
            """;

    public static String withLocal(String base, String vocab, String term) {
        return PREFIXES + "@prefix v: <" + base + vocab + "#> .\n" + """
                <urn:shape:Test> a sh:NodeShape ;
                  sh:property [ sh:path simpl:uuid ; sh:datatype xsd:string ] ;
                  sh:property [ sh:path simpl:version ; sh:datatype xsd:string ] ;
                  sh:property [ sh:path v:%s ; sh:datatype xsd:string ] .
                """.formatted(term);
    }

    public static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private Fixtures() {
    }
}
