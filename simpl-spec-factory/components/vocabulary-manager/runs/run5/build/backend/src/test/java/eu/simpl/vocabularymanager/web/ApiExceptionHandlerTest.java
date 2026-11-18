package eu.simpl.vocabularymanager.web;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.store.StoreException;
import eu.simpl.vocabularymanager.store.StoreUnavailableException;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import tools.jackson.databind.ObjectMapper;

/** Error outcomes without leaking internals (D24, D44, BLG-err-sanit). */
class ApiExceptionHandlerTest {

    private final ProblemWriter writer = new ProblemWriter(new ObjectMapper());
    private final ApiExceptionHandler handler = new ApiExceptionHandler(writer);

    @Test
    void storeErrorIsInternalServerErrorWithoutDetails() {
        var response = handler.storeError(new StoreException("Store query failed with status 500"));
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().type()).isEqualTo("urn:problem-type:belgif:internalServerError");
        assertThat(response.getBody().detail()).isEqualTo("Internal Server Error");
    }

    @Test
    void unexpectedErrorIsInternalServerError() {
        var response = handler.unexpected(new IllegalStateException("secret internals"));
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().detail()).doesNotContain("secret");
    }

    @Test
    void storeUnavailableIs503WithRetryAfter() {
        var response = handler.storeUnavailable(new StoreUnavailableException("down", null));
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("5");
    }

    @Test
    void containerErrorsGetProblemBodies() {
        ProblemErrorController controller = new ProblemErrorController(writer);
        for (int status : new int[] {400, 404, 405, 413, 500}) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
            assertThat(controller.error(request).getStatusCode().value()).isEqualTo(status);
        }
    }

    @Test
    void identityProviderDownIs503() {
        ApiException e = ApiException.serviceUnavailable("The identity provider is not available. Retry later.");
        assertThat(writer.entity(e).getStatusCode().value()).isEqualTo(503);
        assertThat(ApiException.methodNotAllowed("GET").headers()).containsEntry("Allow", "GET");
        assertThat(ApiException.notAcceptable().problem().type()).isEqualTo("urn:problem-type:simpl:notAcceptable");
    }
}
