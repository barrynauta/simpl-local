package eu.simpl.vocabularymanager.contract;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.support.ApiTestBase;
import eu.simpl.vocabularymanager.web.HandlerRuleVerifier;
import eu.simpl.vocabularymanager.web.Operation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

/**
 * D84, D93, D133, D206: the operation table, the Spring MVC handlers, the proxy map and openapi-decided.yaml list
 * the same operations, with the same role rules and limits.
 */
class ContractTest extends ApiTestBase {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @SuppressWarnings("unchecked")
    private static Map<String, Object> decided() throws IOException {
        return new Yaml().load(Files.readString(Path.of("../docs/openapi-decided.yaml")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void tableMatchesTheDecidedContractAndTheHandlers() throws IOException {
        Map<String, Object> paths = (Map<String, Object>) decided().get("paths");
        Set<String> contract = new HashSet<>();
        Map<String, List<String>> roles = new java.util.HashMap<>();
        for (Map.Entry<String, Object> path : paths.entrySet()) {
            for (Map.Entry<String, Object> method : ((Map<String, Object>) path.getValue()).entrySet()) {
                String key = method.getKey().toUpperCase() + " /v1" + path.getKey();
                contract.add(key);
                Map<String, Object> op = (Map<String, Object>) method.getValue();
                roles.put(key, (List<String>) op.get("x-roles"));
                assertThat(((Map<String, Object>) op.get("responses")).keySet()).contains("405");
            }
        }
        Set<String> table = new HashSet<>();
        for (Operation operation : Operation.values()) {
            String key = operation.method() + " " + operation.path();
            table.add(key);
            List<String> expected = switch (operation.access()) {
                case PUBLIC -> List.of();
                case VIEWER -> List.of("GA_VOCABULARY_VIEWER", "GA_VOCABULARY_ADMIN");
                case ADMIN -> List.of("GA_VOCABULARY_ADMIN");
            };
            assertThat(roles.get(key)).as(key).containsExactlyInAnyOrderElementsOf(expected);
        }
        assertThat(table).isEqualTo(contract);
        assertThat(HandlerRuleVerifier.handlerKeys(handlerMapping)).isEqualTo(table);
    }

    @Test
    @SuppressWarnings("unchecked")
    void limitsMatchTheDecidedContract() throws IOException {
        Map<String, Object> components = (Map<String, Object>) decided().get("components");
        Map<String, Object> parameters = (Map<String, Object>) components.get("parameters");
        Map<String, Object> pageSize =
                (Map<String, Object>) ((Map<String, Object>) parameters.get("PageSizeQuery")).get("schema");
        assertThat(pageSize.get("maximum")).isEqualTo(eu.simpl.vocabularymanager.web.Paging.MAX_PAGE_SIZE);
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        Map<String, Object> create =
                (Map<String, Object>) ((Map<String, Object>) schemas.get("CreateVocabularyPayload")).get("properties");
        assertThat(((Map<String, Object>) create.get("description")).get("maxLength"))
                .isEqualTo(eu.simpl.vocabularymanager.web.RequestRules.MAX_TEXT);
        assertThat(((Map<String, Object>) create.get("vocabularyFile")).get("maxLength"))
                .isEqualTo((int) eu.simpl.vocabularymanager.web.MultipartForm.FILE_LIMIT);
        assertThat(((Map<String, Object>) create.get("name")).get("pattern"))
                .isEqualTo(eu.simpl.vocabularymanager.domain.VocabularyNames.PATTERN.pattern());
    }

    @Test
    void proxyAllowMapMatchesTheTable() throws IOException {
        String nginx = Files.readString(Path.of("../ui/nginx/nginx.conf"));
        Matcher matcher = Pattern.compile("~\\^(\\S+)\\$ \"([A-Z, ]+)\";").matcher(nginx);
        int rows = 0;
        while (matcher.find()) {
            rows++;
            String regex = matcher.group(1);
            String allow = matcher.group(2);
            String sample = regex.replace("[^/]+", "Abc").replace("\\", "");
            List<String> expected = new java.util.ArrayList<>();
            for (Operation operation : Operation.values()) {
                String pathRegex = operation.path().replaceAll("\\{[^}]+}", "[^/]+");
                if (pathRegex.equals(regex)) {
                    expected.add(operation.method());
                }
            }
            assertThat(allow).as(sample).isEqualTo(String.join(", ", expected));
        }
        assertThat(rows).isEqualTo(13);
    }
}
