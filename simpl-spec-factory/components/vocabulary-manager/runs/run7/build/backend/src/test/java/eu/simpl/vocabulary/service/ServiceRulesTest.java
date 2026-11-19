package eu.simpl.vocabulary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.simpl.vocabulary.api.problem.ApiException;
import eu.simpl.vocabulary.domain.model.Vocabulary;
import eu.simpl.vocabulary.domain.model.VocabularyStatus;
import eu.simpl.vocabulary.domain.model.VocabularyVersion;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Sorting (D23, D58), paging (D22), date filters (D20), locks (D43, D71). */
class ServiceRulesTest {

    static Vocabulary vocabulary(String name, VocabularyStatus status, int createdDay, int updatedDay) {
        return new Vocabulary(
                name,
                status,
                "u",
                Instant.parse("2026-01-01T00:00:00Z").plus(Duration.ofDays(createdDay)),
                Instant.parse("2026-01-01T00:00:00Z").plus(Duration.ofDays(updatedDay)),
                1,
                "d");
    }

    @Test
    void vocabularyOrder() {
        List<Vocabulary> list = new ArrayList<>(List.of(
                vocabulary("Ab", VocabularyStatus.ACTIVE, 2, 1),
                vocabulary("AZ", VocabularyStatus.DEPRECATED, 1, 3),
                vocabulary("Ba", VocabularyStatus.ACTIVE, 1, 2)));
        list.sort(VocabularyService.vocabularyOrder(null));
        assertThat(list).extracting(Vocabulary::name).containsExactly("AZ", "Ab", "Ba");
        list.sort(VocabularyService.vocabularyOrder("-name"));
        assertThat(list).extracting(Vocabulary::name).containsExactly("Ba", "Ab", "AZ");
        list.sort(VocabularyService.vocabularyOrder("status"));
        assertThat(list).extracting(Vocabulary::name).containsExactly("Ab", "Ba", "AZ");
        list.sort(VocabularyService.vocabularyOrder("-status"));
        assertThat(list).extracting(Vocabulary::name).containsExactly("AZ", "Ab", "Ba");
        list.sort(VocabularyService.vocabularyOrder("creationDate"));
        assertThat(list).extracting(Vocabulary::name).containsExactly("AZ", "Ba", "Ab");
        list.sort(VocabularyService.vocabularyOrder("-lastUpdated"));
        assertThat(list).extracting(Vocabulary::name).containsExactly("AZ", "Ba", "Ab");
    }

    static VocabularyVersion version(long number, VocabularyStatus status, int day) {
        Instant time = Instant.parse("2026-01-01T00:00:00Z").plus(Duration.ofDays(day));
        return new VocabularyVersion("N", number, "d", "c", status, "u", time, time, "h");
    }

    @Test
    void versionOrder() {
        List<VocabularyVersion> list = new ArrayList<>(List.of(
                version(1, VocabularyStatus.DEPRECATED, 1),
                version(3, VocabularyStatus.ACTIVE, 3),
                version(2, VocabularyStatus.DEPRECATED, 2)));
        list.sort(VocabularyService.versionOrder(null));
        assertThat(list).extracting(VocabularyVersion::version).containsExactly(3L, 2L, 1L);
        list.sort(VocabularyService.versionOrder("name"));
        assertThat(list).extracting(VocabularyVersion::version).containsExactly(3L, 2L, 1L);
        list.sort(VocabularyService.versionOrder("status"));
        assertThat(list).extracting(VocabularyVersion::version).containsExactly(3L, 2L, 1L);
        list.sort(VocabularyService.versionOrder("-status"));
        assertThat(list).extracting(VocabularyVersion::version).containsExactly(2L, 1L, 3L);
        list.sort(VocabularyService.versionOrder("creationDate"));
        assertThat(list).extracting(VocabularyVersion::version).containsExactly(1L, 2L, 3L);
        list.sort(VocabularyService.versionOrder("-creationDate"));
        assertThat(list).extracting(VocabularyVersion::version).containsExactly(3L, 2L, 1L);
        assertThat(VocabularyService.sha256(new byte[0])).hasSize(64);
        assertThat(VocabularyService.utf8("a".getBytes())).isEqualTo("a");
    }

    @Test
    void pages() {
        List<Integer> all = List.of(1, 2, 3, 4, 5);
        assertThat(Page.of(all, 1, 2).items()).containsExactly(1, 2);
        assertThat(Page.of(all, 3, 2).items()).containsExactly(5);
        assertThat(Page.of(all, 4, 2).items()).isEmpty();
        assertThat(Page.of(all, Long.MAX_VALUE / 2, 100).items()).isEmpty();
        assertThat(Page.of(all, 1, 2).totalPages()).isEqualTo(3);
        assertThat(Page.of(List.of(), 1, 20).totalPages()).isZero();
        assertThat(Page.of(List.of(), 1, 20).items()).isEmpty();
    }

    @Test
    void dateFilters() {
        assertThat(DateFilter.parse("2026-01-02").orElseThrow().matches(Instant.parse("2026-01-02T23:59:59Z")))
                .isTrue();
        assertThat(DateFilter.parse("2026-01-02").orElseThrow().matches(Instant.parse("2026-01-03T00:00:00Z")))
                .isFalse();
        assertThat(DateFilter.parse("2026-01-02T10:00:00Z")
                        .orElseThrow()
                        .matches(Instant.parse("2026-01-02T10:00:00Z")))
                .isTrue();
        assertThat(DateFilter.parse("2026-01-02T12:00:00+02:00")
                        .orElseThrow()
                        .matches(Instant.parse("2026-01-02T10:00:00Z")))
                .isTrue();
        assertThat(DateFilter.parse("2026-01-02T10:00:00.900Z")
                        .orElseThrow()
                        .matches(Instant.parse("2026-01-02T10:00:00Z")))
                .isTrue();
        assertThat(DateFilter.parse("2026-01-02T10:00:00").orElseThrow().matches(Instant.parse("2026-01-02T10:00:00Z")))
                .isTrue();
        assertThat(DateFilter.parse("2026-02-30")).isEmpty();
        assertThat(DateFilter.parse("2026-01-02Tgarbage")).isEmpty();
        assertThat(DateFilter.parse("02-01-2026")).isEmpty();
        assertThat(DateFilter.parse(null)).isEmpty();
    }

    @Test
    void locksSerialiseAndTimeOut() throws Exception {
        NameLocks locks = new NameLocks(Duration.ofMillis(100));
        CountDownLatch inside = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = new Thread(() -> locks.withLock("Name", () -> {
            inside.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        }));
        holder.start();
        inside.await();
        assertThatThrownBy(() -> locks.withLock("NAME", () -> 1))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.status()).isEqualTo(503));
        assertThat(locks.withLock("Other", () -> 2)).isEqualTo(2);
        release.countDown();
        holder.join();
        AtomicInteger counter = new AtomicInteger();
        assertThat(locks.withLock("name", counter::incrementAndGet)).isEqualTo(1);
        assertThat(locks.size()).isZero();
    }
}
