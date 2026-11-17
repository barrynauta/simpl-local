package eu.simpl.vocabulary.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UploadTest {

    @ParameterizedTest // D15
    @CsvSource(nullValues = "null", value = {
            "v.ttl, text/turtle, true",
            "v.ttl, application/octet-stream, true",
            "v.TTL, null, true",
            "v, text/turtle; charset=utf-8, true",
            "null, application/x-turtle, true",
            "null, text/plain, true",
            "v, application/octet-stream, true",
            "v.json, application/json, false",
            "v.rdf, application/rdf+xml, false",
            "v.xml, application/octet-stream, false",
            "v.txt, text/plain, false",
            "null, application/json, false"})
    void turtleDetection(String filename, String contentType, boolean expected) {
        assertThat(new Upload(filename, contentType, new byte[0]).looksLikeTurtle()).isEqualTo(expected);
    }
}
