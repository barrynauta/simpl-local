package eu.simpl.vocabulary.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure lifecycle rules for vocabularies and versions. No I/O: every method takes the current state and
 * returns the new state, or throws a {@link DomainException}.
 */
public final class VocabularyLifecycle {

    private VocabularyLifecycle() {}

    /** SIMPL-18170, SIMPL-18186, D7: a new vocabulary gets version 1, ACTIVE, and is ACTIVE itself. */
    public static Vocabulary create(String name, String description, String changelog, String user, Instant now) {
        VocabularyVersion v1 = new VocabularyVersion(1, description, changelog, Status.ACTIVE, user, now, now);
        return new Vocabulary(name, Status.ACTIVE, user, now, now, List.of(v1));
    }

    /**
     * SIMPL-18173, 18190, 18191, D7, D9, D11: the next version number is the highest plus one; the new version is
     * ACTIVE; all previous versions become DEPRECATED. A DEPRECATED vocabulary cannot get a new version.
     */
    public static Vocabulary addVersion(Vocabulary current, String description, String changelog, String user,
                                        Instant now) {
        if (current.status() == Status.DEPRECATED) {
            throw DomainException.conflict("Vocabulary '" + current.name()
                    + "' is DEPRECATED. Set the vocabulary status to ACTIVE before creating a new version.");
        }
        int next = current.latestVersionNumber() + 1;
        List<VocabularyVersion> versions = new ArrayList<>();
        for (VocabularyVersion v : current.versions()) {
            versions.add(v.withStatus(Status.DEPRECATED, now));
        }
        versions.add(new VocabularyVersion(next, description, changelog, Status.ACTIVE, user, now, now));
        return new Vocabulary(current.name(), Status.ACTIVE, current.createdBy(), current.creationDate(), now,
                versions);
    }

    /**
     * SIMPL-18175, D10: DEPRECATED sets the vocabulary and all versions to DEPRECATED. ACTIVE sets the vocabulary
     * and only its latest version to ACTIVE. Requesting the current status is a conflict.
     */
    public static Vocabulary changeStatus(Vocabulary current, Status target, Instant now) {
        if (current.status() == target) {
            throw DomainException.conflict("Vocabulary is already in status " + target);
        }
        List<VocabularyVersion> versions = new ArrayList<>();
        int latest = current.latestVersionNumber();
        for (VocabularyVersion v : current.versions()) {
            if (target == Status.DEPRECATED) {
                versions.add(v.withStatus(Status.DEPRECATED, now));
            } else {
                versions.add(v.withStatus(v.version() == latest ? Status.ACTIVE : Status.DEPRECATED, now));
            }
        }
        return new Vocabulary(current.name(), target, current.createdBy(), current.creationDate(), now, versions);
    }

    /**
     * OpenAPI updateVocabularyVersionStatus, D10: a version can only become ACTIVE when no other version of the
     * same vocabulary is ACTIVE. Requesting the current status is a conflict. The vocabulary status is not changed.
     */
    public static Vocabulary changeVersionStatus(Vocabulary current, int versionNumber, Status target, Instant now) {
        VocabularyVersion version = current.version(versionNumber).orElseThrow(() -> DomainException.notFound(
                "Version " + versionNumber + " of vocabulary '" + current.name() + "' was not found."));
        if (version.status() == target) {
            throw DomainException.conflict("Vocabulary version is already in status " + target);
        }
        if (target == Status.ACTIVE) {
            current.versions().stream()
                    .filter(v -> v.version() != versionNumber && v.status() == Status.ACTIVE)
                    .findFirst()
                    .ifPresent(other -> {
                        throw DomainException.conflict("Version " + other.version() + " of vocabulary '"
                                + current.name() + "' is ACTIVE. Only one version can be ACTIVE at a time.");
                    });
        }
        List<VocabularyVersion> versions = current.versions().stream()
                .map(v -> v.version() == versionNumber ? v.withStatus(target, now) : v)
                .toList();
        return new Vocabulary(current.name(), current.status(), current.createdBy(), current.creationDate(), now,
                versions);
    }
}
