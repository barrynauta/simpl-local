package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.domain.Vocabulary;
import eu.simpl.vocabularymanager.domain.VocabularyVersion;
import java.util.Comparator;
import java.util.Optional;

/** The sort values of the list operations (decision D23). */
public final class SortOrder {

    private SortOrder() {}

    private static final Comparator<Vocabulary> BY_NAME = Comparator.comparing(Vocabulary::name);
    private static final Comparator<VocabularyVersion> BY_VERSION_DESC =
            Comparator.comparingInt(VocabularyVersion::version).reversed();

    /** Order of GET /vocabularies; ties are broken by name ascending. Empty for an unknown value. */
    public static Optional<Comparator<Vocabulary>> vocabularies(String sort) {
        if (sort == null) {
            return Optional.of(BY_NAME);
        }
        boolean descending = sort.startsWith("-");
        Comparator<Vocabulary> primary = switch (descending ? sort.substring(1) : sort) {
            case "name" -> BY_NAME;
            case "status" -> Comparator.comparing(v -> v.status().name());
            case "creationDate" -> Comparator.comparing(Vocabulary::creationDate);
            case "lastUpdated" -> Comparator.comparing(Vocabulary::lastUpdateDate);
            default -> null;
        };
        if (primary == null) {
            return Optional.empty();
        }
        return Optional.of((descending ? primary.reversed() : primary).thenComparing(BY_NAME));
    }

    /** Order of GET .../versions; default version descending, ties by version descending. */
    public static Optional<Comparator<VocabularyVersion>> versions(String sort) {
        if (sort == null) {
            return Optional.of(BY_VERSION_DESC);
        }
        boolean descending = sort.startsWith("-");
        Comparator<VocabularyVersion> primary = switch (descending ? sort.substring(1) : sort) {
            case "name" -> Comparator.comparing(VocabularyVersion::name);
            case "status" -> Comparator.comparing(v -> v.status().name());
            case "creationDate" -> Comparator.comparing(VocabularyVersion::creationDate);
            default -> null;
        };
        if (primary == null) {
            return Optional.empty();
        }
        return Optional.of((descending ? primary.reversed() : primary).thenComparing(BY_VERSION_DESC));
    }
}
