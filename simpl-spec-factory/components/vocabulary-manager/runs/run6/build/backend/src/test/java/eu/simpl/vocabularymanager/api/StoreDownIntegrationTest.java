package eu.simpl.vocabularymanager.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.simpl.vocabularymanager.security.SecurityConfig;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockPart;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/** Decision D44: with the store unreachable, reads and writes give 503 with Retry-After, health gives DOWN. */
@SpringBootTest(properties = "vm.store.dataset-url=http://127.0.0.1:1/vocabulary")
@AutoConfigureMockMvc
class StoreDownIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void storeDownGives503() throws Exception {
        var admin = jwt().authorities(new SimpleGrantedAuthority(SecurityConfig.ADMIN));
        mvc.perform(get("/v1/vocabularies").with(admin))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "5"))
                .andExpect(jsonPath("$.type").value("urn:problem-type:belgif:serviceUnavailable"))
                .andExpect(jsonPath("$.detail").value("The vocabulary store is not available. Retry later."));
        MockMultipartHttpServletRequestBuilder create = multipart("/v1/vocabularies");
        create.part(new MockPart("vocabularyFile", "v.ttl", "<http://a> <http://b> <http://c> .".getBytes()));
        for (String[] f : new String[][] {{"name", "StoreDown"}, {"description", "d"}, {"changelog", "c"}}) {
            create.part(new MockPart(f[0], f[1].getBytes(StandardCharsets.UTF_8)));
        }
        mvc.perform(create.with(admin)).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/v1/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }
}
