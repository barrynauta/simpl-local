package eu.simpl.vocabularymanager.web;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.validation.Names;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * D93: the own request validation is compared with docs/openapi-decided.yaml: every operation,
 * its access, query parameters, limits and patterns.
 */
class DecidedContractTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> contract() throws Exception {
        try (InputStream in = Files.newInputStream(Path.of("../docs/openapi-decided.yaml"))) {
            return new Yaml().load(in);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void everyOperationHasARouteWithTheSameParameters() throws Exception {
        Map<String, Object> api = contract();
        Map<String, Map<String, Object>> parameters =
                (Map<String, Map<String, Object>>) ((Map<String, Object>) api.get("components")).get("parameters");
        Set<String> seen = new HashSet<>();
        Map<String, Map<String, Object>> paths = (Map<String, Map<String, Object>>) api.get("paths");
        for (var path : paths.entrySet()) {
            for (var op : path.getValue().entrySet()) {
                Map<String, Object> operation = (Map<String, Object>) op.getValue();
                String method = op.getKey().toUpperCase();
                ApiRoutes.Route route = ApiRoutes.all().stream()
                        .filter(r -> r.method().equals(method) && r.template().equals(path.getKey()))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("no route for " + method + " " + path.getKey()));
                assertThat(route.operationId()).isEqualTo(operation.get("operationId"));
                seen.add(route.operationId());
                Set<String> query = new HashSet<>();
                for (Map<String, Object> p :
                        (List<Map<String, Object>>) operation.getOrDefault("parameters", List.of())) {
                    Map<String, Object> resolved = p.containsKey("$ref")
                            ? parameters.get(((String) p.get("$ref")).replace("#/components/parameters/", ""))
                            : p;
                    if ("query".equals(resolved.get("in"))) {
                        query.add((String) resolved.get("name"));
                    }
                }
                assertThat(route.queryParameters()).as(route.operationId()).isEqualTo(query);
                boolean secured = !(operation.get("security") instanceof List<?> list && list.isEmpty());
                assertThat(route.access() != ApiRoutes.Access.PUBLIC)
                        .as(route.operationId())
                        .isEqualTo(secured);
            }
        }
        assertThat(seen).hasSize(ApiRoutes.all().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void limitsAndPatternsMatchTheCode() throws Exception {
        Map<String, Object> components = (Map<String, Object>) contract().get("components");
        Map<String, Map<String, Object>> parameters = (Map<String, Map<String, Object>>) components.get("parameters");
        Map<String, Object> pageSize =
                (Map<String, Object>) parameters.get("PageSizeQuery").get("schema");
        assertThat(pageSize.get("maximum")).isEqualTo(100);
        assertThat(Names.positiveInt("100", 100)).isEqualTo(100);
        assertThat(Names.positiveInt("101", 100)).isEqualTo(-1);
        Map<String, Object> page =
                (Map<String, Object>) parameters.get("PageQuery").get("schema");
        assertThat(((Number) page.get("maximum")).longValue()).isEqualTo(Integer.MAX_VALUE);
        Map<String, Object> name =
                (Map<String, Object>) parameters.get("VocabularyNamePath").get("schema");
        assertThat(name.get("pattern")).isEqualTo("^[A-Z][a-zA-Z0-9]*$");
        assertThat(name.get("minLength")).isEqualTo(Names.NAME_MIN);
        assertThat(name.get("maxLength")).isEqualTo(Names.NAME_MAX);
        Map<String, Object> filter =
                (Map<String, Object>) parameters.get("VocabularyNameQuery").get("schema");
        assertThat(filter.get("pattern")).isEqualTo("^[A-Za-z0-9]+$");
        Map<String, Map<String, Object>> schemas = (Map<String, Map<String, Object>>) components.get("schemas");
        Map<String, Object> items = (Map<String, Object>)
                ((Map<String, Object>) schemas.get("VocabularySearchRequest").get("properties")).get("items");
        assertThat(items.get("maxItems")).isEqualTo(SearchRequests.MAX_REFERENCES);
        Map<String, Object> aliases = (Map<String, Object>) ((Map<String, Object>)
                        schemas.get("CreateExternalVocabularyPayload").get("properties"))
                .get("aliasUris");
        assertThat(aliases.get("maxItems")).isEqualTo(ExternalVocabularyController.MAX_ALIASES);
        Map<String, Object> description = (Map<String, Object>)
                ((Map<String, Object>) schemas.get("CreateVocabularyPayload").get("properties")).get("description");
        assertThat(description.get("maxLength")).isEqualTo(Names.TEXT_MAX);
    }
}
