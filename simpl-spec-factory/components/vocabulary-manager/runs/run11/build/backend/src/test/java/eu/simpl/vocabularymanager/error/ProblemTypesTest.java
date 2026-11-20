package eu.simpl.vocabularymanager.error;

import static org.assertj.core.api.Assertions.assertThat;

import eu.simpl.vocabularymanager.web.MultipartForm;
import org.junit.jupiter.api.Test;

/** Problem types (D48) and container error mapping. */
class ProblemTypesTest {

    @Test
    void statusesMapToTypes() {
        assertThat(ProblemErrorController.typeFor(404)).isEqualTo(ProblemType.NOT_FOUND);
        assertThat(ProblemErrorController.typeFor(418)).isEqualTo(ProblemType.BAD_REQUEST);
        assertThat(ProblemErrorController.typeFor(599)).isEqualTo(ProblemType.INTERNAL_SERVER_ERROR);
        for (ProblemType type : ProblemType.values()) {
            String prefix = type.status() == 400 || type.status() == 404 || type.status() >= 500
                    ? "urn:problem-type:belgif:"
                    : "urn:problem-type:simpl:";
            assertThat(type.type()).startsWith(prefix);
        }
    }

    @Test
    void multipartFailuresAreTranslated() {
        Exception size = new IllegalStateException(new FakeSizeLimitExceededException());
        assertThat(MultipartForm.translate(size).problemType()).isEqualTo(ProblemType.PAYLOAD_TOO_LARGE);
        Exception count = new IllegalStateException(new FakeFileCountLimitExceededException());
        assertThat(MultipartForm.translate(count).problemType()).isEqualTo(ProblemType.BAD_REQUEST);
        assertThat(MultipartForm.translate(new java.io.IOException("x")).problemType())
                .isEqualTo(ProblemType.BAD_REQUEST);
    }

    static class FakeSizeLimitExceededException extends RuntimeException {}

    static class FakeFileCountLimitExceededException extends RuntimeException {}
}
