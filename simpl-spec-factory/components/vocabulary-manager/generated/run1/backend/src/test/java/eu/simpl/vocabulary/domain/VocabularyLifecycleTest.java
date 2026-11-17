package eu.simpl.vocabulary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class VocabularyLifecycleTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-01-02T00:00:00Z");
    private static final Instant T2 = Instant.parse("2026-01-03T00:00:00Z");

    @Test
    void createMakesVersionOneActive() { // SIMPL-18170, D7
        Vocabulary v = VocabularyLifecycle.create("Health", "d", "Initial version", "ga-admin", T0);
        assertThat(v.status()).isEqualTo(Status.ACTIVE);
        assertThat(v.versions()).hasSize(1);
        assertThat(v.latest().version()).isEqualTo(1);
        assertThat(v.latest().status()).isEqualTo(Status.ACTIVE);
        assertThat(v.createdBy()).isEqualTo("ga-admin");
    }

    @Test
    void newVersionIncrementsAndDeprecatesAllPrevious() { // SIMPL-18173, 18190, 18191, D9
        Vocabulary v = VocabularyLifecycle.create("Health", "d1", "c1", "u", T0);
        v = VocabularyLifecycle.addVersion(v, "d2", "c2", "u", T1);
        v = VocabularyLifecycle.addVersion(v, "d3", "c3", "u", T2);
        assertThat(v.versions()).extracting(VocabularyVersion::version).containsExactly(1, 2, 3);
        assertThat(v.versions()).extracting(VocabularyVersion::status)
                .containsExactly(Status.DEPRECATED, Status.DEPRECATED, Status.ACTIVE);
        assertThat(v.description()).isEqualTo("d3");
        assertThat(v.lastUpdateDate()).isEqualTo(T2);
        assertThat(v.creationDate()).isEqualTo(T0);
    }

    @Test
    void newVersionOnDeprecatedVocabularyIsAConflict() { // D11
        Vocabulary v = VocabularyLifecycle.changeStatus(
                VocabularyLifecycle.create("Health", "d", "c", "u", T0), Status.DEPRECATED, T1);
        assertThatThrownBy(() -> VocabularyLifecycle.addVersion(v, "d", "c", "u", T2))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).kind()).isEqualTo(DomainException.Kind.CONFLICT);
    }

    @Test
    void deprecatingVocabularyDeprecatesAllVersions() { // D10
        Vocabulary v = VocabularyLifecycle.addVersion(VocabularyLifecycle.create("Health", "d", "c", "u", T0),
                "d", "c", "u", T1);
        Vocabulary deprecated = VocabularyLifecycle.changeStatus(v, Status.DEPRECATED, T2);
        assertThat(deprecated.status()).isEqualTo(Status.DEPRECATED);
        assertThat(deprecated.versions()).allMatch(x -> x.status() == Status.DEPRECATED);
    }

    @Test
    void reactivatingVocabularyReactivatesOnlyLatestVersion() { // D10
        Vocabulary v = VocabularyLifecycle.addVersion(VocabularyLifecycle.create("Health", "d", "c", "u", T0),
                "d", "c", "u", T1);
        v = VocabularyLifecycle.changeStatus(v, Status.DEPRECATED, T1);
        v = VocabularyLifecycle.changeStatus(v, Status.ACTIVE, T2);
        assertThat(v.status()).isEqualTo(Status.ACTIVE);
        assertThat(v.versions()).extracting(VocabularyVersion::status)
                .containsExactly(Status.DEPRECATED, Status.ACTIVE);
    }

    @Test
    void changingToTheCurrentStatusIsAConflict() {
        Vocabulary v = VocabularyLifecycle.create("Health", "d", "c", "u", T0);
        assertThatThrownBy(() -> VocabularyLifecycle.changeStatus(v, Status.ACTIVE, T1))
                .isInstanceOf(DomainException.class).hasMessageContaining("already in status ACTIVE");
    }

    @Test
    void versionCannotBecomeActiveWhileAnotherIsActive() { // D10
        Vocabulary v = VocabularyLifecycle.addVersion(VocabularyLifecycle.create("Health", "d", "c", "u", T0),
                "d", "c", "u", T1);
        assertThatThrownBy(() -> VocabularyLifecycle.changeVersionStatus(v, 1, Status.ACTIVE, T2))
                .isInstanceOf(DomainException.class).hasMessageContaining("Only one version can be ACTIVE");
    }

    @Test
    void versionCanBeReactivatedWhenNoOtherIsActive() {
        Vocabulary v = VocabularyLifecycle.addVersion(VocabularyLifecycle.create("Health", "d", "c", "u", T0),
                "d", "c", "u", T1);
        v = VocabularyLifecycle.changeVersionStatus(v, 2, Status.DEPRECATED, T1);
        v = VocabularyLifecycle.changeVersionStatus(v, 1, Status.ACTIVE, T2);
        assertThat(v.version(1).orElseThrow().status()).isEqualTo(Status.ACTIVE);
        assertThat(v.version(2).orElseThrow().status()).isEqualTo(Status.DEPRECATED);
        assertThat(v.status()).isEqualTo(Status.ACTIVE);
    }

    @Test
    void unknownVersionIsNotFound() {
        Vocabulary v = VocabularyLifecycle.create("Health", "d", "c", "u", T0);
        assertThatThrownBy(() -> VocabularyLifecycle.changeVersionStatus(v, 9, Status.DEPRECATED, T1))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).kind()).isEqualTo(DomainException.Kind.NOT_FOUND);
    }
}
