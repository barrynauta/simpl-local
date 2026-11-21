package eu.simpl.vocabularymanager.web;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.web.OperationCatalog.Access;
import eu.simpl.vocabularymanager.web.OperationCatalog.Route;
import org.junit.jupiter.api.Test;

class OperationCatalogTest {

    @Test
    void literalPathsWinOverTemplates() {
        Route search = OperationCatalog.route("POST", "/v1/vocabularies/searches");
        assertThat(search.operation())
                .hasValueSatisfying(op -> assertThat(op.operationId()).isEqualTo("searchVocabularies"));
        Route get = OperationCatalog.route("GET", "/v1/vocabularies/searches");
        assertThat(get.pathKnown()).isTrue();
        assertThat(get.operation()).isEmpty();
        assertThat(get.allowedMethods()).containsExactly("POST");
        Route content = OperationCatalog.route("GET", "/v1/external-vocabularies/content");
        assertThat(content.allowedMethods()).containsExactly("POST");
    }

    @Test
    void templatesAndUnknownPaths() {
        assertThat(OperationCatalog.route("PATCH", "/v1/vocabularies/Abc/versions/2")
                        .operation())
                .hasValueSatisfying(op -> assertThat(op.access()).isEqualTo(Access.WRITE));
        assertThat(OperationCatalog.route("PUT", "/v1/vocabularies").allowedMethods())
                .containsExactly("GET", "POST");
        assertThat(OperationCatalog.route("GET", "/v1/nothing").pathKnown()).isFalse();
        assertThat(OperationCatalog.route("GET", "/v1/vocabularies/").pathKnown())
                .isFalse();
        assertThat(OperationCatalog.route("GET", "/v1/health").operation())
                .hasValueSatisfying(op -> assertThat(op.access()).isEqualTo(Access.PUBLIC));
        assertThat(OperationCatalog.operations()).hasSize(19);
        assertThat(OperationCatalog.byTemplate("DELETE", "/v1/external-vocabularies/{name}"))
                .isPresent();
    }
}
