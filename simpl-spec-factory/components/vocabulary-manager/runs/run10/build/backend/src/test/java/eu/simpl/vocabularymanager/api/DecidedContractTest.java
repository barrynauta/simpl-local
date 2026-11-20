package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.request.FieldRules;
import eu.simpl.vocabularymanager.request.MultipartForm;
import eu.simpl.vocabularymanager.web.ApiOperation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * D93: the own request validation is compared with docs/openapi-decided.yaml: every operation, its role rule, its
 * parameters, every limit and pattern the code enforces.
 */
class DecidedContractTest {

    private static Map<String, Object> api;

    @BeforeAll
    static void load() throws IOException {
        try (InputStream in = Files.newInputStream(Path.of("../docs/openapi-decided.yaml"))) {
            api = new Yaml().load(in);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    private static Map<String, Object> at(Map<String, Object> root, String... keys) {
        Map<String, Object> node = root;
        for (String key : keys) {
            node = map(node.get(key));
        }
        return node;
    }

    @Test
    void operationsMatchTheTable() {
        Map<String, String> contract = new HashMap<>();
        at(api, "paths")
                .forEach((path, item) -> map(item)
                        .forEach((method, operation) -> contract.put(method.toUpperCase() + " /v1" + path, (String)
                                map(operation).get("operationId"))));
        Map<String, String> table = new HashMap<>();
        for (ApiOperation operation : ApiOperation.values()) {
            table.put(operation.method() + " " + operation.pathTemplate(), operation.operationId());
        }
        assertThat(table).isEqualTo(contract);
    }

    @Test
    void securedOperationsDeclare401And403AndHealthIsPublic() {
        for (ApiOperation operation : ApiOperation.values()) {
            Map<String, Object> declared = operation(operation);
            Set<String> statuses = map(declared.get("responses")).keySet();
            if (operation.access() == ApiOperation.Access.PUBLIC) {
                assertThat(declared.get("security")).isEqualTo(List.of());
            } else {
                assertThat(statuses).contains("401", "403", "503");
            }
        }
    }

    @Test
    void queryParametersMatch() {
        assertThat(queryParameters(ApiOperation.GET_VOCABULARIES)).isEqualTo(VocabularyController.LIST_PARAMETERS);
        assertThat(queryParameters(ApiOperation.GET_VOCABULARY_VERSIONS)).isEqualTo(Set.of("sort", "page", "pageSize"));
        assertThat(queryParameters(ApiOperation.GET_EXTERNAL_VOCABULARIES)).isEqualTo(Set.of("page", "pageSize"));
        for (ApiOperation operation : ApiOperation.values()) {
            if (operation != ApiOperation.GET_VOCABULARIES
                    && operation != ApiOperation.GET_VOCABULARY_VERSIONS
                    && operation != ApiOperation.GET_EXTERNAL_VOCABULARIES) {
                assertThat(queryParameters(operation)).as(operation.name()).isEmpty();
            }
        }
    }

    @Test
    void limitsAndPatternsMatch() {
        Map<String, Object> parameters = at(api, "components", "parameters");
        Map<String, Object> namePath = map(at(parameters, "VocabularyNamePath").get("schema"));
        assertThat(namePath.get("pattern")).isEqualTo(FieldRules.NAME.pattern());
        assertThat(namePath.get("minLength")).isEqualTo(FieldRules.NAME_MIN);
        assertThat(namePath.get("maxLength")).isEqualTo(FieldRules.NAME_MAX);
        assertThat(map(at(parameters, "ExternalVocabularyNamePath").get("schema"))
                        .get("pattern"))
                .isEqualTo(FieldRules.NAME.pattern());
        Map<String, Object> nameQuery =
                map(at(parameters, "VocabularyNameQuery").get("schema"));
        assertThat(nameQuery.get("pattern")).isEqualTo(FieldRules.NAME_FILTER.pattern());
        assertThat(nameQuery.get("maxLength")).isEqualTo(FieldRules.NAME_MAX);
        Map<String, Object> pageSize = map(at(parameters, "PageSizeQuery").get("schema"));
        assertThat(pageSize.get("maximum")).isEqualTo(FieldRules.PAGE_SIZE_MAX);
        assertThat(pageSize.get("default")).isEqualTo(FieldRules.PAGE_SIZE_DEFAULT);
        assertThat(map(at(parameters, "PageQuery").get("schema")).get("maximum"))
                .isEqualTo(Integer.MAX_VALUE);
        assertThat(Set.copyOf(
                        (List<?>) map(at(parameters, "VocabularyListSortQuery").get("schema"))
                                .get("enum")))
                .isEqualTo(VocabularyController.LIST_SORTS);
        assertThat(Set.copyOf(
                        (List<?>) map(at(parameters, "SortQuery").get("schema")).get("enum")))
                .isEqualTo(VocabularyController.VERSION_SORTS);

        Map<String, Object> schemas = at(api, "components", "schemas");
        assertThat(at(schemas, "CreateVocabularyPayload", "properties").keySet())
                .isEqualTo(VocabularyController.CREATE_FIELDS);
        assertThat(at(schemas, "CreateNewVocabularyVersionPayload", "properties")
                        .keySet())
                .isEqualTo(VocabularyController.VERSION_FIELDS);
        assertThat(at(schemas, "CreateExternalVocabularyPayload", "properties").keySet())
                .isEqualTo(ExternalVocabularyController.FIELDS);
        assertThat(at(schemas, "ExternalVocabularySearchRequest", "properties").keySet())
                .isEqualTo(ExternalVocabularyController.SEARCH_FIELDS);
        assertThat(at(schemas, "CreateVocabularyPayload", "properties", "description")
                        .get("maxLength"))
                .isEqualTo(FieldRules.TEXT_MAX);
        assertThat(at(schemas, "CreateNewVocabularyVersionPayload", "properties", "changelog")
                        .get("maxLength"))
                .isEqualTo(FieldRules.TEXT_MAX);
        assertThat(at(schemas, "CreateExternalVocabularyPayload", "properties", "aliasUris")
                        .get("maxItems"))
                .isEqualTo(FieldRules.ALIAS_MAX);
        assertThat(at(schemas, "VocabularySearchRequest", "properties", "items").get("maxItems"))
                .isEqualTo(VocabularyController.SEARCH_MAX);
        assertThat(at(schemas, "ExternalVocabularySearchRequest", "properties", "aliasUris")
                        .get("maxItems"))
                .isEqualTo(ExternalVocabularyController.SEARCH_MAX);
        assertThat(at(schemas, "VocabularySearchItemRequest", "properties", "version")
                        .get("maximum"))
                .isEqualTo(Integer.MAX_VALUE);
        Set<String> warningCodes = Set.copyOf((List<String>)
                at(schemas, "VocabularyValidationWarning", "properties", "code").get("enum"));
        assertThat(warningCodes)
                .containsExactlyInAnyOrder(
                        "externalReferenceSkipped", "deprecatedVocabularyReference", "semanticReasoningSkipped");
        assertThat(MultipartForm.FILE_LIMIT).isEqualTo(5_242_880L);
    }

    @Test
    void responseObjectsCarryTheDecidedFields() {
        Map<String, Object> schemas = at(api, "components", "schemas");
        Set<String> metadata = at(schemas, "VocabularyMetadata", "properties").keySet();
        assertThat(metadata).contains("createdBy", "latestVersionHref");
        Set<String> created = new HashSet<>(
                at(schemas, "VocabularyVersionMetadataResponse", "properties").keySet());
        assertThat(created).contains("createdBy", "latestVersionHref", "validationWarnings");
        Set<String> recordFields =
                List.of(eu.simpl.vocabularymanager.api.dto.Dtos.VocabularyMetadata.class.getRecordComponents()).stream()
                        .map(component -> component.getName())
                        .collect(Collectors.toSet());
        assertThat(recordFields).isEqualTo(metadata);
    }

    private static Map<String, Object> operation(ApiOperation operation) {
        String path = operation.pathTemplate().substring("/v1".length());
        return at(api, "paths", path, operation.method().toLowerCase());
    }

    @SuppressWarnings("unchecked")
    private static Set<String> queryParameters(ApiOperation operation) {
        Map<String, Object> parameters = at(api, "components", "parameters");
        Set<String> names = new HashSet<>();
        List<Object> declared = (List<Object>) operation(operation).getOrDefault("parameters", List.of());
        for (Object item : declared) {
            Map<String, Object> parameter = map(item);
            if (parameter.containsKey("$ref")) {
                String ref = (String) parameter.get("$ref");
                parameter = map(parameters.get(ref.substring(ref.lastIndexOf('/') + 1)));
            }
            if ("query".equals(parameter.get("in"))) {
                names.add((String) parameter.get("name"));
            }
        }
        return names;
    }
}
