package eu.simpl.vocabularymanager;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.api.RequestRules;
import eu.simpl.vocabularymanager.web.OperationCatalog;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * D93: the own request validation is compared with docs/openapi-decided.yaml: every operation with its
 * access rule, and the limits and patterns of the parameters and fields.
 */
class ContractConformanceTest {

    private static Map<String, Object> api;

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void load() throws IOException {
        try (InputStream in = Files.newInputStream(Path.of("../docs/openapi-decided.yaml"))) {
            api = (Map<String, Object>) new Yaml().load(in);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    private static Map<String, Object> at(String... keys) {
        Map<String, Object> current = api;
        for (String key : keys) {
            current = map(current.get(key));
        }
        return current;
    }

    @Test
    void everyOperationHasAHandlerRuleAndTheSameRoles() {
        Set<String> declared = new HashSet<>();
        for (Map.Entry<String, Object> path : at("paths").entrySet()) {
            for (Map.Entry<String, Object> method : map(path.getValue()).entrySet()) {
                Map<String, Object> operation = map(method.getValue());
                String verb = method.getKey().toUpperCase();
                var rule = OperationCatalog.byTemplate(verb, OperationCatalog.API_BASE + path.getKey());
                assertThat(rule).as(verb + " " + path.getKey()).isPresent();
                assertThat(rule.get().operationId()).isEqualTo(operation.get("operationId"));
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) operation.get("x-roles");
                OperationCatalog.Access expected = roles.isEmpty()
                        ? OperationCatalog.Access.PUBLIC
                        : roles.contains("GA_VOCABULARY_VIEWER")
                                ? OperationCatalog.Access.READ
                                : OperationCatalog.Access.WRITE;
                assertThat(rule.get().access()).as(rule.get().operationId()).isEqualTo(expected);
                declared.add(verb + " " + path.getKey());
            }
        }
        assertThat(declared).hasSameSizeAs(OperationCatalog.operations());
    }

    @Test
    void limitsAndPatternsMatchTheValidation() {
        Map<String, Object> parameters = at("components", "parameters");
        Map<String, Object> pageSize = map(map(parameters.get("PageSizeQuery")).get("schema"));
        assertThat(pageSize.get("maximum")).isEqualTo(RequestRules.PAGE_SIZE_MAX);
        assertThat(pageSize.get("default")).isEqualTo(RequestRules.PAGE_SIZE_DEFAULT);
        assertThat(map(map(parameters.get("PageQuery")).get("schema")).get("maximum"))
                .isEqualTo((int) RequestRules.INT32_MAX);
        Map<String, Object> nameFilter =
                map(map(parameters.get("VocabularyNameQuery")).get("schema"));
        assertThat(nameFilter.get("pattern")).isEqualTo(RequestRules.NAME_FILTER_PATTERN);
        assertThat(nameFilter.get("maxLength")).isEqualTo(RequestRules.NAME_FILTER_MAX);
        Map<String, Object> namePath =
                map(map(parameters.get("VocabularyNamePath")).get("schema"));
        assertThat(namePath.get("pattern")).isEqualTo(RequestRules.NAME_PATTERN);
        assertThat(namePath.get("minLength")).isEqualTo(RequestRules.NAME_MIN);
        assertThat(namePath.get("maxLength")).isEqualTo(RequestRules.NAME_MAX);
        assertThat(map(map(parameters.get("CreationDateQuery")).get("schema")).get("pattern"))
                .isEqualTo(RequestRules.DATE_PATTERN);

        Map<String, Object> schemas = at("components", "schemas");
        Map<String, Object> create =
                map(map(schemas.get("CreateVocabularyPayload")).get("properties"));
        assertThat(map(create.get("description")).get("maxLength")).isEqualTo(RequestRules.TEXT_MAX);
        assertThat(map(create.get("changelog")).get("maxLength")).isEqualTo(RequestRules.TEXT_MAX);
        Map<String, Object> external =
                map(map(schemas.get("CreateExternalVocabularyPayload")).get("properties"));
        assertThat(map(external.get("aliasUris")).get("maxItems")).isEqualTo(RequestRules.MAX_ALIASES);
        Map<String, Object> search =
                map(map(schemas.get("VocabularySearchRequest")).get("properties"));
        assertThat(map(search.get("items")).get("maxItems")).isEqualTo(100);
    }

    @Test
    void queryParametersPerOperationMatch() {
        Map<String, Object> list = map(map(at("paths").get("/vocabularies")).get("get"));
        assertThat(list.get("parameters").toString())
                .contains(
                        "VocabularyNameQuery",
                        "VocabularyStatusQuery",
                        "CreationDateQuery",
                        "LastUpdatedQuery",
                        "VocabularyListSortQuery",
                        "PageQuery",
                        "PageSizeQuery");
        Map<String, Object> sort = map(map(at("components", "parameters").get("VocabularyListSortQuery"))
                .get("schema"));
        assertThat((List<?>) sort.get("enum")).hasSize(8);
    }
}
