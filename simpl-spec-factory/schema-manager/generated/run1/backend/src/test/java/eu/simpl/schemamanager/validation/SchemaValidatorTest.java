package eu.simpl.schemamanager.validation;

import static eu.simpl.schemamanager.Fixtures.bytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.simpl.schemamanager.Fixtures;
import eu.simpl.schemamanager.config.Settings;
import eu.simpl.schemamanager.domain.SchemaType;
import eu.simpl.schemamanager.web.ApiException;
import eu.simpl.schemamanager.web.Issue;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchemaValidatorTest {

    static final String BASE = "https://vocabulary.simpl.test/content/";

    VocabularyClient client;
    SchemaValidator validator;

    @BeforeEach
    void setUp() {
        client = mock(VocabularyClient.class);
        Settings settings = new Settings("http://localhost/v1", "http://kc", "simpl", "", "http://vm", BASE,
                "https://simpl-programme.ec.europa.eu/ns/default#uuid,https://simpl-programme.ec.europa.eu/ns/default#version",
                "from@x", "p@x", "http://i", "http://ui");
        validator = new SchemaValidator(settings, client);
    }

    private ApiException fail(String ttl, SchemaType type) {
        try {
            validator.validate(bytes(ttl), type);
        } catch (ApiException e) {
            return e;
        }
        throw new AssertionError("expected a validation failure");
    }

    @Test
    void validDocumentPassesWithoutWarningsOrVocabularyCalls() {
        List<Issue> warnings = validator.validate(bytes(Fixtures.VALID), SchemaType.RESOURCE);
        assertThat(warnings).isEmpty();
        verify(client, never()).fetch(anyString());
    }

    @Test
    void syntaxErrorHasPrefixAndPosition() {
        ApiException e = fail(Fixtures.BROKEN, SchemaType.RESOURCE);
        assertThat(e.status().value()).isEqualTo(400);
        assertThat(e.detail()).startsWith("Syntax Error: ");
    }

    @Test
    void missingNodeShapeIsASyntaxError() {
        ApiException e = fail(Fixtures.NO_SHAPE, SchemaType.RESOURCE);
        assertThat(e.detail()).isEqualTo("Syntax Error: no sh:NodeShape found");
    }

    @Test
    void emptyDocumentIsASyntaxError() {
        assertThat(fail("", SchemaType.SEARCH).detail()).startsWith("Syntax Error: ");
    }

    @Test
    void missingDefaultPropertyListsEachMissingIri() {
        ApiException e = fail(Fixtures.NO_VERSION, SchemaType.RESOURCE);
        assertThat(e.detail()).startsWith("Default Properties Error: ");
        assertThat(e.issues()).hasSize(1);
        assertThat(e.issues().get(0).code()).isEqualTo("missingDefaultProperty");
        assertThat(e.issues().get(0).location()).isEqualTo("https://simpl-programme.ec.europa.eu/ns/default#version");
    }

    @Test
    void searchSchemasNeedNoDefaultProperties() {
        assertThat(validator.validate(bytes(Fixtures.NO_VERSION), SchemaType.SEARCH)).isEmpty();
    }

    @Test
    void unknownLocalVocabularyIsASemanticError() {
        when(client.fetch("Vabc")).thenReturn(Optional.empty());
        ApiException e = fail(Fixtures.withLocal(BASE, "Vabc", "name"), SchemaType.RESOURCE);
        assertThat(e.detail()).startsWith("Semantic Validation Error: ");
        assertThat(e.issues()).extracting(Issue::code).containsExactly("localReferenceUnavailable");
        assertThat(e.issues().get(0).location()).contains(BASE + "Vabc#name");
    }

    @Test
    void unknownTermIsASemanticError() {
        when(client.fetch("Vabc")).thenReturn(Optional.of(bytes("<" + BASE + "Vabc#other> a <urn:x:T> .")));
        ApiException e = fail(Fixtures.withLocal(BASE, "Vabc", "name"), SchemaType.RESOURCE);
        assertThat(e.issues()).extracting(Issue::code).containsExactly("unknownVocabularyTerm");
    }

    @Test
    void knownTermPasses() {
        when(client.fetch("Vabc")).thenReturn(Optional.of(bytes("<" + BASE + "Vabc#name> a <urn:x:T> .")));
        assertThat(validator.validate(bytes(Fixtures.withLocal(BASE, "Vabc", "name")), SchemaType.RESOURCE)).isEmpty();
    }

    @Test
    void allErrorsAreCollected() {
        when(client.fetch("Vabc")).thenReturn(Optional.empty());
        String ttl = Fixtures.withLocal(BASE, "Vabc", "name").replace("sh:datatype xsd:string ] .",
                "sh:datatype xsd:string ; sh:node <urn:shape:Missing> ] .");
        ApiException e = fail(ttl, SchemaType.RESOURCE);
        assertThat(e.issues()).extracting(Issue::code)
                .containsExactlyInAnyOrder("localReferenceUnavailable", "unresolvedShapeReference");
    }

    @Test
    void vocabularyManagerOutageGives500() {
        when(client.fetch("Vabc")).thenThrow(new VocabularyUnavailableException("down", null));
        assertThatThrownBy(() -> validator.validate(bytes(Fixtures.withLocal(BASE, "Vabc", "name")), SchemaType.RESOURCE))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.status().value()).isEqualTo(500);
                    assertThat(e.detail()).isEqualTo("Semantic validation unavailable: vocabulary manager unreachable");
                });
    }

    @Test
    void externalPrefixGivesWarning() {
        String ttl = "@prefix ex: <https://example.org/vocab/> .\n" + Fixtures.VALID;
        List<Issue> warnings = validator.validate(bytes(ttl), SchemaType.RESOURCE);
        assertThat(warnings).singleElement().satisfies(w -> {
            assertThat(w.code()).isEqualTo("externalReferenceSkipped");
            assertThat(w.location()).isEqualTo("prefix 'ex' -> 'https://example.org/vocab/'");
        });
    }

    @Test
    void vocabularyNameStopsAtHashOrSlash() {
        assertThat(validator.vocabularyName(BASE + "Foo#bar")).isEqualTo("Foo");
        assertThat(validator.vocabularyName(BASE + "Foo/bar")).isEqualTo("Foo");
    }
}
