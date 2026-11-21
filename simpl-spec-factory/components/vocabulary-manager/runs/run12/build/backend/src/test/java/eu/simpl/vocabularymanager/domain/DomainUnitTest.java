package eu.simpl.vocabularymanager.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.config.RouteRuleCheck;
import eu.simpl.vocabularymanager.domain.Model.Status;
import eu.simpl.vocabularymanager.domain.Model.Vocabulary;
import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemType;
import eu.simpl.vocabularymanager.store.Content;
import eu.simpl.vocabularymanager.store.VocabularyStore;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

/** Lifecycle rules with a mocked store: retries (D82, D157), limits (D92), default deny (D84). */
class DomainUnitTest {

    static final Instant T = Instant.parse("2026-11-20T10:00:00Z");

    VocabularyStore store = mock(VocabularyStore.class);
    ContentValidator validator = mock(ContentValidator.class);
    VocabularyService service =
            new VocabularyService(store, validator, Duration.ofSeconds(30), Clock.fixed(T, ZoneOffset.UTC));

    Vocabulary voc(int latest, Status status) {
        return new Vocabulary("Abc", status, latest, "d", "u", T, T);
    }

    @Test
    void conditionalWriteIsRetriedWithFreshState() {
        when(store.find("Abc")).thenReturn(Optional.of(voc(1, Status.ACTIVE)), Optional.of(voc(2, Status.ACTIVE)));
        when(store.fingerprint("Abc", 1)).thenReturn(Optional.empty());
        when(validator.validateInternal(any(), eq("Abc"), anyInt())).thenReturn(List.of());
        when(store.insertVersion(eq("Abc"), eq(1), any(), anyString())).thenReturn(false);
        when(store.insertVersion(eq("Abc"), eq(2), any(), anyString())).thenReturn(true);
        VocabularyService.Created c = service.createVersion("Abc", "new", "log", new byte[] {1}, "u");
        assertThat(c.version().version()).isEqualTo(3);
        verify(validator, times(1)).validateInternal(any(), any(), anyInt());
    }

    @Test
    void retriesEndIn503() {
        when(store.find("Abc")).thenReturn(Optional.of(voc(1, Status.ACTIVE)));
        when(store.fingerprint("Abc", 1)).thenReturn(Optional.empty());
        when(store.insertVersion(any(), anyInt(), any(), anyString())).thenReturn(false);
        assertThatThrownBy(() -> service.createVersion("Abc", "new", "log", new byte[] {1}, "u"))
                .isInstanceOf(ApiException.class)
                .satisfies(
                        e -> assertThat(((ApiException) e).problemType()).isEqualTo(ProblemType.SERVICE_UNAVAILABLE));
    }

    @Test
    void identicalVersionIs409() {
        byte[] bytes = "x".getBytes();
        when(store.find("Abc")).thenReturn(Optional.of(voc(4, Status.ACTIVE)));
        when(store.fingerprint("Abc", 4))
                .thenReturn(Optional.of(new Model.ContentFingerprint(Content.sha256(bytes), 1, "same")));
        assertThatThrownBy(() -> service.createVersion("Abc", " same ", "log", bytes, "u"))
                .hasMessage("Version 4 of vocabulary 'Abc' already has this content and description.");
    }

    @Test
    void statusChangeRaceGives409OnRetry() {
        when(store.find("Abc")).thenReturn(Optional.of(voc(1, Status.ACTIVE)), Optional.of(voc(1, Status.DEPRECATED)));
        when(store.changeVocabularyStatus(any(), any(), any(), any(), anyString()))
                .thenReturn(false);
        assertThatThrownBy(() -> service.changeStatus("Abc", Status.DEPRECATED))
                .hasMessage("Vocabulary is already in status DEPRECATED");
    }

    @Test
    void createLostRaceIs409() {
        when(store.nameKeyExists("Abc")).thenReturn(false);
        when(store.insertVocabulary(any(), any(), anyString())).thenReturn(false);
        assertThatThrownBy(() -> service.create("Abc", "d", "c", new byte[0], "u"))
                .hasMessage("Vocabulary with name 'Abc' already exists.");
    }

    @Test
    void uploadLimiter() {
        UploadLimiter limiter = new UploadLimiter(new AppProperties(null, null, null, new AppProperties.Uploads(1)));
        UploadLimiter.Permit p = limiter.acquire();
        assertThatThrownBy(limiter::acquire).hasMessage(ApiException.TOO_MANY_UPLOADS);
        p.close();
        limiter.acquire().close();
    }

    @Test
    void writeRetryBackoff() {
        WriteRetry r = new WriteRetry(Duration.ofSeconds(30));
        for (int i = 0; i < 5; i++) {
            r.beforeRetry();
        }
        assertThat(r.attempts()).isEqualTo(5);
        assertThatThrownBy(r::beforeRetry).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> new WriteRetry(Duration.ofMillis(1)).beforeRetry())
                .isInstanceOf(ApiException.class);
    }

    @Test
    void handlerWithoutRuleFailsTheStart() {
        RequestMappingInfo rogue =
                RequestMappingInfo.paths("/v1/rogue").methods(RequestMethod.GET).build();
        HandlerMethod hm = mock(HandlerMethod.class);
        assertThatThrownBy(() -> RouteRuleCheck.check(Map.of(rogue, hm)))
                .hasMessageContaining("Handler without role rule");
        RequestMappingInfo noMethod = RequestMappingInfo.paths("/v1/x").build();
        assertThatThrownBy(() -> RouteRuleCheck.check(Map.of(noMethod, hm))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> RouteRuleCheck.check(Map.of())).hasMessageContaining("Role rule without handler");
    }

    @Test
    void contentChunksRoundTrip() {
        byte[] big = new byte[3 * 1024 * 1024 + 7];
        big[5] = 9;
        assertThat(Content.fromChunks(Content.toChunks(big))).isEqualTo(big);
        assertThat(Content.toChunks(big)).hasSize(4);
        assertThat(Content.fromChunks(Content.toChunks(new byte[0]))).isEmpty();
        assertThat(ProblemType.forStatus(418)).isEqualTo(ProblemType.BAD_REQUEST);
        assertThat(ProblemType.forStatus(599)).isEqualTo(ProblemType.INTERNAL_ERROR);
    }
}
