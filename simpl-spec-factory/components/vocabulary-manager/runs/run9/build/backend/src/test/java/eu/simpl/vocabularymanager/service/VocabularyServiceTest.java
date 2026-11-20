package eu.simpl.vocabularymanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.domain.ConflictException;
import eu.simpl.vocabularymanager.domain.ContentValidationException;
import eu.simpl.vocabularymanager.domain.ExternalVocabulary;
import eu.simpl.vocabularymanager.domain.ExternalVocabularyNotFoundException;
import eu.simpl.vocabularymanager.domain.SearchReference;
import eu.simpl.vocabularymanager.domain.ServiceBusyException;
import eu.simpl.vocabularymanager.domain.ValidationIssue;
import eu.simpl.vocabularymanager.domain.VersionNotFoundException;
import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyNotFoundException;
import eu.simpl.vocabularymanager.domain.VocabularyStatus;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import eu.simpl.vocabularymanager.store.ExternalVocabularyRepository;
import eu.simpl.vocabularymanager.store.VocabularyRepository;
import eu.simpl.vocabularymanager.validation.ContentValidator;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Lifecycle rules with the store mocked: order of checks, retries of D82, conflicts, upload slots of D92. */
class VocabularyServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-19T10:00:00Z");
    private static final byte[] CONTENT = "content".getBytes(StandardCharsets.UTF_8);

    private final VocabularyRepository repository = mock(VocabularyRepository.class);
    private final ContentValidator validator = mock(ContentValidator.class);
    private final VocabularyManagerProperties properties = properties(4, Duration.ofMillis(50), Duration.ofSeconds(5));
    private final UploadSlots slots = new UploadSlots(properties);
    private final VocabularyService service =
            new VocabularyService(repository, validator, slots, Clock.fixed(NOW, ZoneOffset.UTC), properties);

    static VocabularyManagerProperties properties(int uploads, Duration wait, Duration budget) {
        return new VocabularyManagerProperties(
                "https://authority.example/vocabulary-manager/",
                new VocabularyManagerProperties.Store(
                        "q",
                        "u",
                        "r",
                        "r",
                        "w",
                        "w",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(10),
                        Duration.ofSeconds(45),
                        Duration.ofSeconds(2),
                        budget),
                new VocabularyManagerProperties.Identity(
                        "iss", "jwks", "aud", Duration.ofSeconds(2), Duration.ofSeconds(10)),
                new VocabularyManagerProperties.Limits(5_242_880, 6_291_456, 1_048_576, uploads, wait));
    }

    private static Vocabulary vocabulary(int latest, VocabularyStatus status, String sha) {
        return new Vocabulary("Name", status, latest, "desc", sha, "creator", NOW, NOW);
    }

    @Test
    void contentBaseHasNoTrailingSlash() {
        assertThat(properties.contentBase()).isEqualTo("https://authority.example/vocabulary-manager");
    }

    @Test
    void createChecksNameBeforeValidation() {
        when(repository.existsIgnoreCase("Name")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new CreateVocabularyCommand("Name", "d", "c", CONTENT, "u")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Vocabulary with name 'Name' already exists.");
        verify(validator, never()).validate(any(), any(), anyInt());
    }

    @Test
    void createLosingTheRaceIsConflict() {
        when(validator.validate(any(), eq("Name"), eq(1))).thenReturn(List.of());
        when(repository.insertVocabulary(any())).thenReturn(false);
        assertThatThrownBy(() -> service.create(new CreateVocabularyCommand("Name", "d", "c", CONTENT, "u")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createStoresVersionOne() {
        when(validator.validate(any(), eq("Name"), eq(1))).thenReturn(List.of());
        when(repository.insertVocabulary(any())).thenReturn(true);
        CreationResult result = service.create(new CreateVocabularyCommand("Name", "d", "c", CONTENT, "u"));
        assertThat(result.version().version()).isEqualTo(1);
        assertThat(result.version().creationDate()).isEqualTo(NOW);
    }

    @Test
    void newVersionRetriesWhenTheLatestVersionMoved() {
        when(repository.find("Name"))
                .thenReturn(Optional.of(vocabulary(1, VocabularyStatus.ACTIVE, "x")))
                .thenReturn(Optional.of(vocabulary(2, VocabularyStatus.ACTIVE, "x")));
        when(validator.validate(any(), eq("Name"), eq(2))).thenReturn(List.of());
        when(repository.insertVersion(any(), eq(1))).thenReturn(false);
        when(repository.insertVersion(any(), eq(2))).thenReturn(true);
        CreationResult result = service.createVersion("Name", new CreateVersionCommand("new", "c", CONTENT, "u"));
        assertThat(result.version().version()).isEqualTo(3);
    }

    @Test
    void newVersionGivesUpAfterTheRetries() {
        when(repository.find("Name")).thenReturn(Optional.of(vocabulary(1, VocabularyStatus.ACTIVE, "x")));
        when(validator.validate(any(), any(), anyInt())).thenReturn(List.of());
        when(repository.insertVersion(any(), anyInt())).thenReturn(false);
        assertThatThrownBy(() -> service.createVersion("Name", new CreateVersionCommand("new", "c", CONTENT, "u")))
                .isInstanceOf(ServiceBusyException.class)
                .hasMessage(ServiceBusyException.STORE_DETAIL);
    }

    @Test
    void identicalVersionIsRefusedBeforeValidation() {
        String sha = VocabularyService.sha256(CONTENT);
        when(repository.find("Name")).thenReturn(Optional.of(vocabulary(4, VocabularyStatus.DEPRECATED, sha)));
        assertThatThrownBy(() -> service.createVersion("Name", new CreateVersionCommand("desc", "c", CONTENT, "u")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Version 4 of vocabulary 'Name' already has this content and description.");
        verify(validator, never()).validate(any(), any(), anyInt());
    }

    @Test
    void validationErrorsStoreNothing() {
        when(repository.find("Name")).thenReturn(Optional.of(vocabulary(1, VocabularyStatus.ACTIVE, "x")));
        when(validator.validate(any(), any(), anyInt()))
                .thenThrow(new ContentValidationException(List.of(new ValidationIssue("d", "document"))));
        assertThatThrownBy(() -> service.createVersion("Name", new CreateVersionCommand("new", "c", CONTENT, "u")))
                .isInstanceOf(ContentValidationException.class);
        verify(repository, never()).insertVersion(any(), anyInt());
    }

    @Test
    void statusChanges() {
        when(repository.find("Name"))
                .thenReturn(Optional.of(vocabulary(1, VocabularyStatus.ACTIVE, "x")))
                .thenReturn(Optional.of(vocabulary(1, VocabularyStatus.DEPRECATED, "x")));
        when(repository.updateVocabularyStatus(any(), any(), any(), any())).thenReturn(false);
        assertThatThrownBy(() -> service.changeStatus("Name", VocabularyStatus.DEPRECATED))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Vocabulary is already in status DEPRECATED");
        when(repository.find("Other")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.changeStatus("Other", VocabularyStatus.ACTIVE))
                .isInstanceOf(VocabularyNotFoundException.class);
    }

    @Test
    void statusChangeGivesUpWhenTheConditionKeepsFailing() {
        when(repository.find("Name")).thenReturn(Optional.of(vocabulary(1, VocabularyStatus.ACTIVE, "x")));
        when(repository.updateVocabularyStatus(any(), any(), any(), any())).thenReturn(false);
        assertThatThrownBy(() -> service.changeStatus("Name", VocabularyStatus.DEPRECATED))
                .isInstanceOf(ServiceBusyException.class);
    }

    @Test
    void versionStatusChanges() {
        VocabularyVersion active =
                new VocabularyVersion("Name", 1, "d", "c", VocabularyStatus.ACTIVE, "u", NOW, NOW, 1);
        VocabularyVersion deprecated =
                new VocabularyVersion("Name", 1, "d", "c", VocabularyStatus.DEPRECATED, "u", NOW, NOW, 1);
        when(repository.findVersion("Name", 1)).thenReturn(Optional.of(active), Optional.of(deprecated));
        when(repository.updateVersionStatus("Name", 1, VocabularyStatus.ACTIVE, VocabularyStatus.DEPRECATED, NOW))
                .thenReturn(true);
        assertThat(service.changeVersionStatus("Name", 1, VocabularyStatus.DEPRECATED)
                        .status())
                .isEqualTo(VocabularyStatus.DEPRECATED);
        when(repository.findVersion("Name", 1)).thenReturn(Optional.of(deprecated));
        assertThatThrownBy(() -> service.changeVersionStatus("Name", 1, VocabularyStatus.DEPRECATED))
                .hasMessage("Vocabulary version is already in status DEPRECATED");
        when(repository.findVersion("Name", 1)).thenReturn(Optional.of(active));
        when(repository.updateVersionStatus(any(), anyInt(), any(), any(), any()))
                .thenReturn(false);
        assertThatThrownBy(() -> service.changeVersionStatus("Name", 1, VocabularyStatus.DEPRECATED))
                .isInstanceOf(ServiceBusyException.class);
    }

    @Test
    void notFoundDistinguishesVocabularyAndVersion() {
        when(repository.findVersion("Name", 9)).thenReturn(Optional.empty());
        when(repository.find("Name")).thenReturn(Optional.of(vocabulary(1, VocabularyStatus.ACTIVE, "x")));
        assertThatThrownBy(() -> service.version("Name", 9)).isInstanceOf(VersionNotFoundException.class);
        when(repository.content("Gone", null)).thenReturn(Optional.empty());
        when(repository.find("Gone")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.content("Gone", null)).isInstanceOf(VocabularyNotFoundException.class);
        assertThatThrownBy(() -> service.versions("Gone", null, false, null))
                .isInstanceOf(VocabularyNotFoundException.class);
    }

    @Test
    void searchDropsRepeatedReferences() {
        service.search(
                List.of(new SearchReference("A", null), new SearchReference("A", 1), new SearchReference("A", null)));
        verify(repository).search(List.of(new SearchReference("A", null), new SearchReference("A", 1)));
    }

    @Test
    void fifthUploadIsRefused() throws Exception {
        UploadSlots one = new UploadSlots(properties(1, Duration.ofMillis(20), Duration.ofSeconds(1)));
        CountDownLatch taken = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            try (UploadSlots.Slot ignored = one.acquire()) {
                taken.countDown();
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        holder.start();
        assertThat(taken.await(5, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(one::acquire)
                .isInstanceOf(ServiceBusyException.class)
                .hasMessage(ServiceBusyException.UPLOADS_DETAIL);
        release.countDown();
        holder.join();
        try (UploadSlots.Slot slot = one.acquire()) {
            slot.close();
        }
    }

    @Test
    void externalService() {
        ExternalVocabularyRepository externals = mock(ExternalVocabularyRepository.class);
        ExternalVocabularyService external =
                new ExternalVocabularyService(externals, validator, Clock.fixed(NOW, ZoneOffset.UTC));
        RegisterExternalCommand command =
                new RegisterExternalCommand("Dc", "http://a.org/", List.of("http://b.org#"), null, null, CONTENT, "u");
        when(externals.existsIgnoreCase("Dc")).thenReturn(false);
        when(externals.existingKeys(any())).thenReturn(Set.of("http://b.org"));
        assertThatThrownBy(() -> external.register(command))
                .hasMessage("An external vocabulary with namespace or alias 'http://b.org#' is already registered.");
        when(externals.existingKeys(any())).thenReturn(Set.of());
        when(externals.insert(any(), any())).thenReturn(true);
        assertThat(external.register(command).creationDate()).isEqualTo(NOW);
        when(externals.insert(any(), any())).thenReturn(false);
        assertThatThrownBy(() -> external.register(command)).isInstanceOf(ConflictException.class);
        when(externals.existsIgnoreCase("Dc")).thenReturn(true);
        assertThatThrownBy(() -> external.register(command))
                .hasMessage("External vocabulary with name 'Dc' already exists.");

        ExternalVocabulary dc = new ExternalVocabulary("Dc", "http://a.org/", List.of(), null, null, "u", NOW);
        when(externals.findByKeys(any())).thenReturn(Map.of("http://a.org", dc));
        when(externals.content("Dc")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> external.contentByNamespace("http://a.org#"))
                .isInstanceOf(ExternalVocabularyNotFoundException.class);
        assertThat(external.search(List.of("http://a.org", "http://a.org/", "http://c.org")))
                .containsExactly(dc);
        when(externals.find("X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> external.delete("X")).isInstanceOf(ExternalVocabularyNotFoundException.class);
        assertThatThrownBy(() -> external.content("X")).isInstanceOf(ExternalVocabularyNotFoundException.class);
        assertThat(external.now()).isEqualTo(NOW);
    }
}
