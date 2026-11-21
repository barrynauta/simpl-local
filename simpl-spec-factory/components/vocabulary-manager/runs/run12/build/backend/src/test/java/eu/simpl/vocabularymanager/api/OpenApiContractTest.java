package eu.simpl.vocabularymanager.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileReader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * D93: the own request validation is compared with docs/openapi-decided.yaml: every operation, its
 * query parameters, form fields, limits and patterns.
 */
class OpenApiContractTest {

    static Map<String, Object> api;

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void load() throws Exception {
        try (FileReader r = new FileReader("../docs/openapi-decided.yaml")) {
            api = new Yaml().load(r);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object o, String key) {
        return (Map<String, Object>) ((Map<String, Object>) o).get(key);
    }

    @SuppressWarnings("unchecked")
    static Object resolve(Object o) {
        if (o instanceof Map<?, ?> m && m.containsKey("$ref")) {
            String[] parts = ((String) m.get("$ref")).substring(2).split("/");
            Object cur = api;
            for (String p : parts) {
                cur = ((Map<String, Object>) cur).get(p);
            }
            return cur;
        }
        return o;
    }

    @Test
    @SuppressWarnings("unchecked")
    void everyOperationIsARoute() {
        Map<String, Object> paths = map(api, "paths");
        Set<String> fromSpec = new HashSet<>();
        for (Map.Entry<String, Object> p : paths.entrySet()) {
            for (Map.Entry<String, Object> op : ((Map<String, Object>) p.getValue()).entrySet()) {
                String opId = (String) ((Map<String, Object>) op.getValue()).get("operationId");
                fromSpec.add(op.getKey().toUpperCase() + " " + Routes.V1 + p.getKey() + " " + opId);
            }
        }
        Set<String> fromCode = new HashSet<>();
        for (Routes.Route r : Routes.ALL) {
            fromCode.add(r.method() + " " + r.pattern() + " " + r.operationId());
        }
        assertThat(fromCode).isEqualTo(fromSpec);
    }

    @Test
    @SuppressWarnings("unchecked")
    void queryParametersAndLimits() {
        Map<String, Object> paths = map(api, "paths");
        List<Object> listParams =
                (List<Object>) map(paths.get("/vocabularies"), "get").get("parameters");
        Set<String> names = new HashSet<>();
        for (Object p : listParams) {
            Map<String, Object> param = (Map<String, Object>) resolve(p);
            names.add((String) param.get("name"));
            if ("sort".equals(param.get("name"))) {
                assertThat(new HashSet<>((List<String>) map(param, "schema").get("enum")))
                        .isEqualTo(VocabularyController.LIST_SORTS);
            }
            if ("pageSize".equals(param.get("name"))) {
                assertThat(map(param, "schema").get("maximum")).isEqualTo(100);
            }
            if ("name".equals(param.get("name"))) {
                assertThat(map(param, "schema").get("pattern")).isEqualTo(Validation.NAME_FILTER.pattern());
            }
        }
        assertThat(names).isEqualTo(VocabularyController.LIST_PARAMS);
        List<Object> versionParams = (List<Object>)
                map(paths.get("/vocabularies/{vocabularyName}/versions"), "get").get("parameters");
        Set<String> vnames = new HashSet<>();
        for (Object p : versionParams) {
            Map<String, Object> param = (Map<String, Object>) resolve(p);
            if ("query".equals(param.get("in"))) {
                vnames.add((String) param.get("name"));
            }
            if ("sort".equals(param.get("name"))) {
                assertThat(new HashSet<>((List<String>) map(param, "schema").get("enum")))
                        .isEqualTo(VocabularyController.VERSION_SORTS);
            }
        }
        assertThat(vnames).isEqualTo(VocabularyController.VERSION_PARAMS);
    }

    @Test
    @SuppressWarnings("unchecked")
    void formFieldsAndPatterns() {
        Map<String, Object> schemas = map(map(api, "components"), "schemas");
        assertThat(map(map(schemas, "CreateVocabularyPayload"), "properties").keySet())
                .isEqualTo(VocabularyController.CREATE_FIELDS);
        assertThat(map(map(schemas, "CreateNewVocabularyVersionPayload"), "properties")
                        .keySet())
                .isEqualTo(VocabularyController.VERSION_FIELDS);
        assertThat(map(map(schemas, "CreateExternalVocabularyPayload"), "properties")
                        .keySet())
                .isEqualTo(ExternalController.FIELDS);
        Map<String, Object> name = map(map(map(schemas, "CreateVocabularyPayload"), "properties"), "name");
        assertThat(name.get("pattern")).isEqualTo(Validation.NAME.pattern());
        assertThat(name.get("minLength")).isEqualTo(3);
        assertThat(name.get("maxLength")).isEqualTo(64);
        Map<String, Object> desc = map(map(map(schemas, "CreateVocabularyPayload"), "properties"), "description");
        assertThat(desc.get("maxLength")).isEqualTo(Validation.TEXT_MAX);
        Map<String, Object> items = map(map(map(schemas, "VocabularySearchRequest"), "properties"), "items");
        assertThat(items.get("maxItems")).isEqualTo(100);
        Map<String, Object> aliases =
                map(map(map(schemas, "CreateExternalVocabularyPayload"), "properties"), "aliasUris");
        assertThat(aliases.get("maxItems")).isEqualTo(20);
    }
}
