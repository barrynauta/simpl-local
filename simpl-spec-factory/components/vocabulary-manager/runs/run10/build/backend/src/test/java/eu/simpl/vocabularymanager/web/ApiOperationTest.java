package eu.simpl.vocabularymanager.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ApiOperationTest {

    @ParameterizedTest
    @CsvSource({
        "GET,/v1/vocabularies,GET_VOCABULARIES",
        "POST,/v1/vocabularies/searches,SEARCH_VOCABULARIES",
        "PATCH,/v1/vocabularies/Abc,UPDATE_VOCABULARY_STATUS",
        "POST,/v1/vocabularies/Abc/versions,CREATE_VOCABULARY_VERSION",
        "GET,/v1/content/Abc/versions/2,DOWNLOAD_VOCABULARY_VERSION",
        "POST,/v1/external-vocabularies/content,DOWNLOAD_EXTERNAL_VOCABULARY_CONTENT",
        "GET,/v1/external-vocabularies/Abc/content,GET_EXTERNAL_VOCABULARY_CONTENT_BY_NAME",
        "DELETE,/v1/external-vocabularies/Abc,DELETE_EXTERNAL_VOCABULARY",
        "GET,/v1/health,GET_HEALTH"
    })
    void routesToTheOperation(String method, String path, ApiOperation expected) {
        assertThat(ApiOperation.route(method, path).orElseThrow().operation()).contains(expected);
    }

    @Test
    void fixedSegmentWinsOverParameter() {
        ApiOperation.Route route =
                ApiOperation.route("GET", "/v1/vocabularies/searches").orElseThrow();
        assertThat(route.operation()).isEmpty();
        assertThat(route.allowedMethods()).isEqualTo("POST");
        assertThat(ApiOperation.route("GET", "/v1/external-vocabularies/content")
                        .orElseThrow()
                        .allowedMethods())
                .isEqualTo("POST");
    }

    @Test
    void unknownPathsAndEmptySegmentsDoNotRoute() {
        assertThat(ApiOperation.route("GET", "/v1/other")).isEmpty();
        assertThat(ApiOperation.route("GET", "/v1/vocabularies/")).isEmpty();
        assertThat(ApiOperation.route("GET", "relative")).isEmpty();
        assertThat(ApiOperation.route("GET", null)).isEmpty();
    }

    @Test
    void rolesFollowAccess() {
        assertThat(ApiOperation.GET_HEALTH.allowedRoles()).isEmpty();
        assertThat(ApiOperation.GET_VOCABULARIES.allowedRoles())
                .containsExactlyInAnyOrder(ApiOperation.ROLE_VIEWER, ApiOperation.ROLE_ADMIN);
        assertThat(ApiOperation.CREATE_VOCABULARY.allowedRoles()).containsExactly(ApiOperation.ROLE_ADMIN);
        assertThat(ApiOperation.SEARCH_EXTERNAL_VOCABULARIES.access()).isEqualTo(ApiOperation.Access.READ);
        assertThat(ApiOperation.CREATE_VOCABULARY.operationId()).isEqualTo("createVocabulary");
    }

    @Test
    void acceptRules() {
        MediaTypeFilter.checkAccept(ApiOperation.GET_VOCABULARIES, Collections.emptyEnumeration());
        MediaTypeFilter.checkAccept(
                ApiOperation.GET_VOCABULARIES, Collections.enumeration(List.of("text/html, application/json;q=0.5")));
        MediaTypeFilter.checkAccept(ApiOperation.DOWNLOAD_VOCABULARY, Collections.enumeration(List.of("text/turtle")));
        assertStatus(
                () -> MediaTypeFilter.checkAccept(
                        ApiOperation.DOWNLOAD_VOCABULARY, Collections.enumeration(List.of("application/json"))),
                406);
        assertStatus(
                () -> MediaTypeFilter.checkAccept(
                        ApiOperation.GET_VOCABULARIES, Collections.enumeration(List.of("application/json;q=0"))),
                406);
        assertStatus(
                () -> MediaTypeFilter.checkAccept(
                        ApiOperation.GET_VOCABULARIES, Collections.enumeration(List.of("garbage;;;"))),
                406);
        assertStatus(
                () -> MediaTypeFilter.checkAccept(
                        ApiOperation.GET_VOCABULARIES, Collections.enumeration(List.of("application/*"))),
                406);
    }

    @Test
    void contentTypeRules() {
        MediaTypeFilter.checkContentType(ApiOperation.UPDATE_VOCABULARY_STATUS, "application/json; charset=utf-8");
        MediaTypeFilter.checkContentType(ApiOperation.CREATE_VOCABULARY, "multipart/form-data; boundary=x");
        MediaTypeFilter.checkContentType(ApiOperation.GET_VOCABULARIES, null);
        assertStatus(() -> MediaTypeFilter.checkContentType(ApiOperation.UPDATE_VOCABULARY_STATUS, null), 415);
        assertStatus(() -> MediaTypeFilter.checkContentType(ApiOperation.CREATE_VOCABULARY, "application/json"), 415);
        assertStatus(() -> MediaTypeFilter.checkContentType(ApiOperation.SEARCH_VOCABULARIES, "bad type"), 415);
    }

    static void assertStatus(Runnable call, int status) {
        try {
            call.run();
        } catch (eu.simpl.vocabularymanager.error.ProblemException e) {
            assertThat(e.status()).isEqualTo(status);
            return;
        }
        throw new AssertionError("expected status " + status);
    }
}
