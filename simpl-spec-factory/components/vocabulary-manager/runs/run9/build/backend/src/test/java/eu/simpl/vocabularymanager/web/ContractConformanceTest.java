package eu.simpl.vocabularymanager.web;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.api.ExternalVocabularyController;
import eu.simpl.vocabularymanager.api.FieldRules;
import eu.simpl.vocabularymanager.api.QueryParsers;
import eu.simpl.vocabularymanager.api.RequestChecks;
import eu.simpl.vocabularymanager.api.SearchController;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * D93: the build validates requests with its own code; this test compares that code with docs/openapi-decided.yaml:
 * every operation, its parameters, the limits and the patterns.
 */
class ContractConformanceTest {

    private final OpenAPI api = new OpenAPIV3Parser().read("src/test/resources/openapi-decided.yaml");
    private final RouteTable routes = new RouteTable();

    private Map<String, Operation> operations() {
        Map<String, Operation> result = new HashMap<>();
        api.getPaths()
                .forEach((path, item) -> item.readOperationsMap()
                        .forEach((method, op) -> result.put(method.name() + " /v1" + path, op)));
        return result;
    }

    @Test
    void everyOperationHasARouteWithItsOperationId() {
        Map<String, Operation> operations = operations();
        Set<String> declared = routes.routes().stream()
                .map(r -> r.method() + " " + r.template())
                .collect(Collectors.toSet());
        assertThat(declared).isEqualTo(operations.keySet());
        for (Route route : routes.routes()) {
            assertThat(operations.get(route.method() + " " + route.template()).getOperationId())
                    .isEqualTo(route.operationId());
        }
    }

    @Test
    void queryParametersAndLimitsMatch() {
        Map<String, Operation> operations = operations();
        Map<String, Set<String>> expected = Map.of(
                "GET /v1/vocabularies",
                        Set.of("name", "status", "creationDate", "lastUpdated", "sort", "page", "pageSize"),
                "GET /v1/vocabularies/{vocabularyName}/versions", Set.of("sort", "page", "pageSize"),
                "GET /v1/external-vocabularies", Set.of("page", "pageSize"));
        operations.forEach((key, op) -> {
            Set<String> query = parameters(op).stream()
                    .filter(p -> "query".equals(p.getIn()))
                    .map(Parameter::getName)
                    .collect(Collectors.toSet());
            assertThat(query).as(key).isEqualTo(expected.getOrDefault(key, Set.of()));
        });
        Map<String, Parameter> list = parameters(operations.get("GET /v1/vocabularies")).stream()
                .collect(Collectors.toMap(Parameter::getName, p -> p));
        assertThat(list.get("pageSize").getSchema().getMaximum())
                .isEqualByComparingTo(BigDecimal.valueOf(QueryParsers.PAGE_SIZE_MAX));
        assertThat(list.get("pageSize").getSchema().getDefault()).isEqualTo(QueryParsers.PAGE_SIZE_DEFAULT);
        assertThat(list.get("page").getSchema().getMaximum())
                .isEqualByComparingTo(BigDecimal.valueOf(RequestChecks.INT32_MAX));
        assertThat(list.get("name").getSchema().getPattern()).isEqualTo(QueryParsers.NAME_FILTER.pattern());
        assertThat(list.get("name").getSchema().getMaxLength()).isEqualTo(QueryParsers.NAME_FILTER_MAX);
        assertThat(list.get("creationDate").getSchema().getPattern()).isEqualTo(QueryParsers.DATE_FILTER.pattern());
        assertThat(list.get("sort").getSchema().getEnum()).isEqualTo(QueryParsers.VOCABULARY_SORTS);
        Map<String, Parameter> versions =
                parameters(operations.get("GET /v1/vocabularies/{vocabularyName}/versions")).stream()
                        .collect(Collectors.toMap(Parameter::getName, p -> p));
        assertThat(versions.get("sort").getSchema().getEnum()).isEqualTo(QueryParsers.VERSION_SORTS);
        Schema<?> name = versions.get("vocabularyName").getSchema();
        assertThat(name.getPattern()).isEqualTo(RequestChecks.NAME.pattern());
        assertThat(name.getMinLength()).isEqualTo(RequestChecks.NAME_MIN);
        assertThat(name.getMaxLength()).isEqualTo(RequestChecks.NAME_MAX);
    }

    @Test
    void bodySchemasMatch() {
        Map<String, Schema> schemas = api.getComponents().getSchemas();
        Schema<?> create = schemas.get("CreateVocabularyPayload");
        assertThat(create.getProperties().keySet())
                .containsExactlyInAnyOrder("vocabularyFile", "name", "description", "changelog");
        assertThat(((Schema<?>) create.getProperties().get("description")).getMaxLength())
                .isEqualTo(FieldRules.TEXT_MAX);
        assertThat(((Schema<?>) create.getProperties().get("name")).getPattern())
                .isEqualTo(RequestChecks.NAME.pattern());
        assertThat(schemas.get("CreateNewVocabularyVersionPayload")
                        .getProperties()
                        .keySet())
                .containsExactlyInAnyOrder("vocabularyFile", "description", "changelog");
        Schema<?> external = schemas.get("CreateExternalVocabularyPayload");
        assertThat(external.getProperties().keySet())
                .containsExactlyInAnyOrder(
                        "vocabularyFile", "name", "namespaceUri", "aliasUris", "description", "sourceUri");
        assertThat(((Schema<?>) external.getProperties().get("aliasUris")).getMaxItems())
                .isEqualTo(ExternalVocabularyController.MAX_ALIASES);
        assertThat(((Schema<?>) schemas.get("VocabularySearchRequest")
                                .getProperties()
                                .get("items"))
                        .getMaxItems())
                .isEqualTo(SearchController.MAX_REFERENCES);
        assertThat(schemas.get("ExternalVocabularySearchRequest")
                        .getProperties()
                        .keySet())
                .containsExactlyInAnyOrder("namespaceUri", "aliasUri", "namespaceUris", "aliasUris");
        assertThat(schemas.get("VocabularyStatusPatchRequest").getProperties().keySet())
                .containsExactly("status");
        assertThat(schemas.get("VocabularyStatus").getEnum()).isEqualTo(List.of("ACTIVE", "DEPRECATED"));
    }

    private static List<Parameter> parameters(Operation op) {
        return op.getParameters() == null ? List.of() : op.getParameters();
    }

    @SuppressWarnings("unused")
    private static PathItem unused() {
        return null;
    }
}
