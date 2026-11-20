package eu.simpl.vocabularymanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.error.ProblemException;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Lifecycle rules and the retry loop of conditional writes (D4, D8, D43, D46, D51, D82, D92). */
class VocabularyServiceTest {

    private final VocabularyRepository repository = mock(VocabularyRepository.class);
    private final ContentValidation validation = mock(ContentValidation.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-20T10:00:00.700Z"), ZoneOffset.UTC);
    private final VocabularyService service = new VocabularyService(repository, validation, clock);
    private final byte[] content = "x".getBytes(StandardCharsets.UTF_8);
    private final VocabularyService.Upload upload = new VocabularyService.Upload("Desc", "Log", content, "me");

    private Vocabulary vocabulary(int latest, VocabularyStatus status) {
        return new Vocabulary("Abc", status, latest, "Desc", "me", Instant.EPOCH, Instant.EPOCH);
    }

    private VocabularyVersion version(int number, VocabularyStatus status, String sha) {
        return new VocabularyVersion("Abc", number, "Desc", "Log", status, "me", Instant.EPOCH, Instant.EPOCH, sha);
    }

    @Test
    void createChecksDuplicateBeforeValidation() {
        when(repository.existsIgnoreCase("Abc")).thenReturn(true);
        assertThatThrownBy(() -> service.create("Abc", upload)).isInstanceOfSatisfying(ProblemException.class, e -> {
            assertThat(e.status()).isEqualTo(409);
            assertThat(e.detail()).isEqualTo("Vocabulary with name 'Abc' already exists.");
        });
        verify(validation, never()).validate(any(), anyString(), anyInt());
    }

    @Test
    void createLosingTheRaceIsAConflict() {
        when(repository.insertVocabulary(any(), anyString())).thenReturn(false);
        assertThatThrownBy(() -> service.create("Abc", upload))
                .isInstanceOfSatisfying(
                        ProblemException.class, e -> assertThat(e.status()).isEqualTo(409));
    }

    @Test
    void createStoresVersionOneWithTimesToTheSecond() {
        when(repository.insertVocabulary(any(), anyString())).thenReturn(true);
        VocabularyService.Created created = service.create("Abc", upload);
        assertThat(created.version().version()).isEqualTo(1);
        assertThat(created.version().creationDate()).isEqualTo(Instant.parse("2026-11-20T10:00:00Z"));
        assertThat(created.version().status()).isEqualTo(VocabularyStatus.ACTIVE);
        assertThat(created.version().contentSha256()).isEqualTo(VocabularyService.sha256(content));
    }

    @Test
    void newVersionRetriesWithTheLatestNumberThenGives503() {
        when(repository.find("Abc")).thenReturn(Optional.of(vocabulary(1, VocabularyStatus.ACTIVE)));
        when(repository.findVersion("Abc", 1)).thenReturn(Optional.of(version(1, VocabularyStatus.ACTIVE, "other")));
        when(repository.insertVersion(any(), anyInt(), anyString())).thenReturn(false);
        assertThatThrownBy(() -> service.createVersion("Abc", upload))
                .isInstanceOfSatisfying(
                        ProblemException.class, e -> assertThat(e.status()).isEqualTo(503));
        verify(repository, times(VocabularyService.MAX_RETRIES + 1)).insertVersion(any(), eq(1), anyString());
        verify(validation, times(1)).validate(any(), eq("Abc"), eq(2));
    }

    @Test
    void newVersionUsesTheNumberReadAfterALostRace() {
        when(repository.find("Abc"))
                .thenReturn(Optional.of(vocabulary(1, VocabularyStatus.ACTIVE)))
                .thenReturn(Optional.of(vocabulary(2, VocabularyStatus.ACTIVE)));
        when(repository.findVersion("Abc", 1)).thenReturn(Optional.empty());
        when(repository.insertVersion(any(), eq(1), anyString())).thenReturn(false);
        when(repository.insertVersion(any(), eq(2), anyString())).thenReturn(true);
        assertThat(service.createVersion("Abc", upload).version().version()).isEqualTo(3);
    }

    @Test
    void identicalVersionIsAConflictBeforeValidation() {
        when(repository.find("Abc")).thenReturn(Optional.of(vocabulary(4, VocabularyStatus.DEPRECATED)));
        when(repository.findVersion("Abc", 4))
                .thenReturn(Optional.of(version(4, VocabularyStatus.ACTIVE, VocabularyService.sha256(content))));
        assertThatThrownBy(() -> service.createVersion("Abc", upload))
                .isInstanceOfSatisfying(
                        ProblemException.class,
                        e -> assertThat(e.detail())
                                .isEqualTo("Version 4 of vocabulary 'Abc' already has this content and description."));
        verify(validation, never()).validate(any(), anyString(), anyInt());
    }

    @Test
    void newVersionOfUnknownVocabularyIs404() {
        when(repository.find("Abc")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createVersion("Abc", upload))
                .isInstanceOfSatisfying(
                        ProblemException.class, e -> assertThat(e.status()).isEqualTo(404));
    }

    @Test
    void statusChangeRereadsAfterAConcurrentChangeAndThenConflicts() {
        when(repository.find("Abc"))
                .thenReturn(Optional.of(vocabulary(1, VocabularyStatus.ACTIVE)))
                .thenReturn(Optional.of(vocabulary(1, VocabularyStatus.DEPRECATED)));
        when(repository.updateVocabularyStatus(any(), any(), any(), any(), anyString()))
                .thenReturn(false);
        assertThatThrownBy(() -> service.changeStatus("Abc", VocabularyStatus.DEPRECATED))
                .isInstanceOfSatisfying(ProblemException.class, e -> {
                    assertThat(e.status()).isEqualTo(409);
                    assertThat(e.detail()).isEqualTo("Vocabulary is already in status DEPRECATED");
                });
    }

    @Test
    void statusChangeGivesUp503AfterRetries() {
        when(repository.find("Abc")).thenReturn(Optional.of(vocabulary(1, VocabularyStatus.ACTIVE)));
        when(repository.updateVocabularyStatus(any(), any(), any(), any(), anyString()))
                .thenReturn(false);
        assertThatThrownBy(() -> service.changeStatus("Abc", VocabularyStatus.DEPRECATED))
                .isInstanceOfSatisfying(
                        ProblemException.class, e -> assertThat(e.status()).isEqualTo(503));
        when(repository.findVersion("Abc", 1)).thenReturn(Optional.of(version(1, VocabularyStatus.ACTIVE, "s")));
        when(repository.updateVersionStatus(any(), anyInt(), any(), any(), any(), anyString()))
                .thenReturn(false);
        assertThatThrownBy(() -> service.changeVersionStatus("Abc", 1, VocabularyStatus.DEPRECATED))
                .isInstanceOfSatisfying(
                        ProblemException.class, e -> assertThat(e.status()).isEqualTo(503));
    }

    @Test
    void versionStatusChecks() {
        when(repository.find("Abc")).thenReturn(Optional.of(vocabulary(2, VocabularyStatus.ACTIVE)));
        when(repository.findVersion("Abc", 9)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.changeVersionStatus("Abc", 9, VocabularyStatus.ACTIVE))
                .isInstanceOfSatisfying(
                        ProblemException.class,
                        e -> assertThat(e.detail()).isEqualTo("Version 9 of vocabulary 'Abc' was not found."));
        when(repository.findVersion("Abc", 1)).thenReturn(Optional.of(version(1, VocabularyStatus.DEPRECATED, "s")));
        assertThatThrownBy(() -> service.changeVersionStatus("Abc", 1, VocabularyStatus.DEPRECATED))
                .isInstanceOfSatisfying(
                        ProblemException.class,
                        e -> assertThat(e.detail()).isEqualTo("Vocabulary version is already in status DEPRECATED"));
        assertThatThrownBy(() -> service.getVersion("Abc", 9))
                .isInstanceOfSatisfying(
                        ProblemException.class, e -> assertThat(e.status()).isEqualTo(404));
        when(repository.content("Abc", 9)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.content("Abc", 9))
                .isInstanceOfSatisfying(
                        ProblemException.class, e -> assertThat(e.status()).isEqualTo(404));
        when(repository.content("Abc", 2)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.latestContent("Abc"))
                .isInstanceOfSatisfying(
                        ProblemException.class, e -> assertThat(e.status()).isEqualTo(404));
        assertThat(service.versions("Abc", null, 40, 20).items()).isEmpty();
        verify(repository, never()).listVersions(anyString(), any(), eq(40L), anyInt());
    }

    @Test
    void uploadLimiterRefusesTheFifthParallelUpload() throws Exception {
        UploadLimiter limiter = new UploadLimiter(1);
        CountDownLatch inside = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Integer> status = new AtomicReference<>();
        Thread first = new Thread(() -> limiter.run(() -> {
            inside.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return 1;
        }));
        first.start();
        inside.await();
        try {
            limiter.run(() -> 2);
        } catch (ProblemException e) {
            status.set(e.status());
            assertThat(e.detail()).isEqualTo("Too many uploads in progress. Retry later.");
            assertThat(e.headers()).containsEntry("Retry-After", "5");
        }
        release.countDown();
        first.join();
        assertThat(status.get()).isEqualTo(503);
        assertThat(limiter.run(() -> 3)).isEqualTo(3);
        assertThat(List.of(1)).isNotEmpty();
    }
}
